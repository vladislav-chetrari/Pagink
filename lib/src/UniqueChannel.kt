import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized

@OptIn(InternalCoroutinesApi::class)
internal class UniqueChannel<T> private constructor(
    private val channel: Channel<T>,
) : Channel<T> by channel {

    private val lock = SynchronizedObject()
    private val seen = mutableSetOf<T>()

    constructor(capacity: Int = RENDEZVOUS) : this(Channel(capacity))

    override fun trySend(element: T): ChannelResult<Unit> = synchronized(lock) {
        if (seen.add(element)) channel.trySend(element)
        else ChannelResult.failure()
    }

    override suspend fun send(element: T) {
        val added = synchronized(lock) { seen.add(element) }
        if (!added) return
        try {
            channel.send(element)
        } catch (e: Throwable) {
            allow(element)
            throw e
        }
    }

    fun allow(element: T) {
        synchronized(lock) {
            seen.remove(element)
        }
    }
}
