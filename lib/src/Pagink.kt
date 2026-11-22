import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Inspired by Dave Leeds coroutine graph: https://youtu.be/qyMPE4bCYr8?si=HT7d2vCWr8HX-AOP

//TODO fix request duplication
object Pagink {

    const val DEFAULT_PAGE_LOADER_INSTANCES = 4

    //K - key type
    //T - page list item type
    fun <K, T> create(
        pageLoaderInstances: Int = DEFAULT_PAGE_LOADER_INSTANCES,
        initialKeys: Set<K?>,
        load: suspend (K?) -> PageData<K, T>,
    ): Flow<List<Page<T>>> = callbackFlow {

        require(pageLoaderInstances > 0) { "pageLoaderInstances should be more than 0" }
        require(initialKeys.isNotEmpty()) { "initialKeys should not be empty!" }

        val dataHolder = DataHolder<K, T>()
        val keyChannel = Channel<K?>()
        val recoverKeyChannel = Channel<K?>()
        val pageDataChannel = Channel<PageData<K, T>>()

        repeat(pageLoaderInstances) {
            launch { PageLoader(keyChannel, pageDataChannel, load).start() }
        }
        launch {
            ErrorToPlaceholderSwapper(dataHolder, recoverKeyChannel, keyChannel).start()
        }
        launch {
            PageOrganiser(initialKeys.first(), dataHolder, keyChannel, recoverKeyChannel, pageDataChannel).start()
        }
        launch {
            dataHolder.pageListFlow.collect(::send)
        }

        for (key in initialKeys) {
            dataHolder.put(key, PPlaceholder(key, keyChannel))
        }

        awaitClose {
            keyChannel.close()
            pageDataChannel.close()
        }
    }

    data class PageData<K, T>(
        val key: Key.Value<K>,
        val prevKey: Key<K> = Key.Unknown,
        val nextKey: Key<K> = Key.Unknown,
        val error: Throwable? = null,
        val data: List<T> = emptyList(),
    ) {

        sealed interface Key<out K> {
            data object Unknown : Key<Nothing>
            data class Value<K>(val value: K?) : Key<K>
        }
    }

    private class PPlaceholder<K>(
        private val key: K?,
        private val keyOutbound: SendChannel<K?>,
    ) : Page.Placeholder {

        override fun load() {
            Logger.e { "Load placeholder with key = $key" }
            keyOutbound.trySend(key)
        }
    }

    private class PError<K>(
        reason: Throwable,
        private val key: K?,
        private val keyOutbound: SendChannel<K?>,
    ) : Page.Error(reason) {

        override fun retry() {
            keyOutbound.trySend(key)
        }
    }

    private class DataHolder<Key, T> {

        //todo solve in one structure
        private val orderStateFlow = MutableStateFlow<Set<Key?>>(emptySet())
        private val pagesStateFlow = MutableStateFlow<Map<Key?, Page<T>>>(emptyMap())

        val pageListFlow: Flow<List<Page<T>>> = combine(orderStateFlow, pagesStateFlow) { order, pagesMap ->
            order.mapNotNull { pagesMap[it] }
        }

        fun contains(key: Key?): Boolean {
            return orderStateFlow.value.contains(key)
        }

        fun put(key: Key?, page: Page<T>) {
            pagesStateFlow.update { it + (key to page) }
            orderStateFlow.update { it + key }
        }

        fun putBefore(refKey: Key?, key: Key?, value: Page<T>) {
            insertRelativeTo(refKey, key, value, after = false)
        }

        fun putAfter(refKey: Key?, key: Key?, value: Page<T>) {
            insertRelativeTo(refKey, key, value, after = true)
        }

        private fun insertRelativeTo(
            refKey: Key?,
            key: Key?,
            value: Page<T>,
            after: Boolean,
        ) {
            if (refKey == key || !contains(refKey)) return

            pagesStateFlow.update { it + (key to value) }
            orderStateFlow.update { old ->
                val newOrder = old.toMutableList()
                newOrder.remove(key)
                val refIndex = newOrder.indexOf(refKey)
                newOrder.add(if (after) refIndex + 1 else refIndex, key)
                newOrder.toSet()
            }
        }
    }

    private class PageLoader<K, T>(
        private val keyInbound: ReceiveChannel<K?>,
        private val pageDataOutbound: SendChannel<PageData<K, T>>,
        private val load: suspend (K?) -> PageData<K, T>
    ) {

        suspend fun start() {
            for (key in keyInbound) {
                try {
                    val pageData = load(key)
                    pageDataOutbound.send(pageData)
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    pageDataOutbound.send(PageData(key = PageData.Key.Value(key), error = t))
                }
            }
        }
    }

    private class PageOrganiser<K, T>(
        private val initialKey: K?,
        private val dataHolder: DataHolder<K, T>,
        private val keyOutbound: SendChannel<K?>,
        private val errorKeyOutbound: SendChannel<K?>,
        private val pageDataInbound: ReceiveChannel<PageData<K, T>>,
    ) {

        suspend fun start() {
            for (pageData in pageDataInbound) {
                val page =
                    if (pageData.error == null) Page.Success(
                        items = pageData.data,
                        isFirst = pageData.prevKey is PageData.Key.Unknown,
                        isLast = pageData.nextKey is PageData.Key.Unknown,
                    )
                    else PError(
                        reason = pageData.error,
                        key = pageData.key.value,
                        keyOutbound = errorKeyOutbound,
                    )

                //insert page
                when {
                    //either first page or error
                    pageData.key.value == initialKey
                            || pageData.error != null -> dataHolder.put(
                        key = pageData.key.value,
                        page = page,
                    )

                    pageData.prevKey is PageData.Key.Value
                            && dataHolder.contains(pageData.prevKey.value) -> dataHolder.putAfter(
                        refKey = pageData.prevKey.value,
                        key = pageData.key.value,
                        value = page,
                    )

                    pageData.nextKey is PageData.Key.Value
                            && dataHolder.contains(pageData.nextKey.value) -> dataHolder.putBefore(
                        refKey = pageData.nextKey.value,
                        key = pageData.key.value,
                        value = page,
                    )
                }

                //add prev page placeholder if needed
                if (pageData.prevKey is PageData.Key.Value
                    && !dataHolder.contains(pageData.prevKey.value)
                ) dataHolder.putBefore(
                    refKey = pageData.key.value,
                    key = pageData.prevKey.value,
                    value = PPlaceholder(pageData.prevKey.value, keyOutbound)
                )

                //add next page placeholder if needed
                if (pageData.nextKey is PageData.Key.Value
                    && !dataHolder.contains(pageData.nextKey.value)
                ) dataHolder.putAfter(
                    refKey = pageData.key.value,
                    key = pageData.nextKey.value,
                    value = PPlaceholder(pageData.nextKey.value, keyOutbound)
                )
            }
        }
    }

    private class ErrorToPlaceholderSwapper<K, T>(
        private val dataHolder: DataHolder<K, T>,
        private val keyInbound: ReceiveChannel<K?>,
        private val keyOutbound: SendChannel<K?>,
    ) {

        suspend fun start() {
            for (key in keyInbound) {
                dataHolder.put(key, PPlaceholder(key, keyOutbound))
            }
        }
    }
}