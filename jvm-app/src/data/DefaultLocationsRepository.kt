package data

import co.touchlab.kermit.Logger
import data.model.LocationPageResponse
import domain.LocationsRepository
import domain.model.Location
import domain.model.Page
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

internal class DefaultLocationsRepository(
    private val httpClient: HttpClient,
) : LocationsRepository {

    override suspend fun readPage(pageKey: String): Page<String, Location> {
        Logger.d { "Loading page $pageKey" }
        val response = httpClient.get(pageKey).body<LocationPageResponse>()
        return Page(
            key = pageKey,
            previousKey = response.pagingInfo.previousPageUrl,
            nextKey = response.pagingInfo.nextPageUrl,
            items = response.items.map { responseItem ->
                Location(
                    id = responseItem.id,
                    name = responseItem.name,
                    type = responseItem.type,
                    dimension = responseItem.dimension,
                )
            },
        )
    }
}