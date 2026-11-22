package domain.model

data class Page<K, T>(
    val key: K? = null,
    val previousKey: K? = null,
    val nextKey: K? = null,
    val items: List<T> = emptyList(),
)
