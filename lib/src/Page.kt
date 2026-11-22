sealed interface Page<out T> {

    fun interface Placeholder : Page<Nothing> {
        fun load()
    }

    data class Success<T>(
        val items: List<T>,
        val isFirst: Boolean = false,
        val isLast: Boolean = false,
    ) : Page<T>

    open class Error(val reason: Throwable) : Page<Nothing> {
        open fun retry() = Unit
    }

    //for custom insets, such as Ads
    interface CustomInset : Page<Nothing>
}
