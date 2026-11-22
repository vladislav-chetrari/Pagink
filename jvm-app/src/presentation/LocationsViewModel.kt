package presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.FlowLocationsPagingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

internal class LocationsViewModel(
    flowLocationsPaging: FlowLocationsPagingUseCase,
) : ViewModel(CoroutineScope(SupervisorJob() + Dispatchers.Default)) {

    val uiState = flowOf(LocationsListUiState())
        .combine(flowLocationsPaging()){ uiState, pages ->
            uiState.copy(pages = pages)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, LocationsListUiState())
}

