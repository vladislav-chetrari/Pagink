package domain

import Page
import Pagink
import domain.model.Location
import kotlinx.coroutines.flow.Flow

class FlowLocationsPagingUseCase(
    private val repository: LocationsRepository,
) {

    operator fun invoke(): Flow<List<Page<Location>>> = Pagink.create(
        initialKeys = setOf(FIRST_PAGE_LOCATIONS, SECOND_PAGE_LOCATIONS, THIRD_PAGE_LOCATIONS),
        load = { key ->
            requireNotNull(key)
            val page = repository.readPage(key)
            Pagink.PageData(
                key = Pagink.PageData.Key.Value(page.key),
                prevKey =
                    if (page.previousKey == null) Pagink.PageData.Key.Unknown
                    else Pagink.PageData.Key.Value(page.previousKey),
                nextKey =
                    if (page.nextKey == null) Pagink.PageData.Key.Unknown
                    else Pagink.PageData.Key.Value(page.nextKey),
                data = page.items,
            )
        }
    )

    companion object {
        private const val FIRST_PAGE_LOCATIONS = "https://rickandmortyapi.com/api/location?page=1"
        private const val SECOND_PAGE_LOCATIONS = "https://rickandmortyapi.com/api/location?page=2"
        private const val THIRD_PAGE_LOCATIONS = "https://rickandmortyapi.com/api/location?page=3"
    }
}