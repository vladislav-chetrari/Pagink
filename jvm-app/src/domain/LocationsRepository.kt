package domain

import domain.model.Location
import domain.model.Page

interface LocationsRepository {

    suspend fun readPage(pageKey: String): Page<String, Location>
}