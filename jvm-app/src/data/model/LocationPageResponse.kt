package data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LocationPageResponse(
    @SerialName("info")
    val pagingInfo: PagingInfo = PagingInfo(),
    @SerialName("results")
    val items: List<Item> = emptyList(),
) {

    @Serializable
    data class PagingInfo(
        @SerialName("count")
        val totalItemsCount: Int = 0,
        @SerialName("pages")
        val pagesCount: Int = 0,
        @SerialName("next")
        val nextPageUrl: String? = null,
        @SerialName("prev")
        val previousPageUrl: String? = null,
    )

    @Serializable
    data class Item(
        @SerialName("id")
        val id: Int = 0,
        @SerialName("name")
        val name: String = "",
        @SerialName("type")
        val type: String = "",
        @SerialName("dimension")
        val dimension: String = "",
    )
}