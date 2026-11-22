package data

import domain.LocationsRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val module = module {
    single<Json>{
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
    }
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }
    single<LocationsRepository> { DefaultLocationsRepository(get()) }
}