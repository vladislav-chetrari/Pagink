package presentation

import Page
import androidx.compose.runtime.Immutable
import domain.model.Location

@Immutable
internal data class LocationsListUiState(
    val pages: List<Page<Location>> = emptyList(),
)