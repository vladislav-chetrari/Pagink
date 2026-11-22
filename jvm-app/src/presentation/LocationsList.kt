package presentation

import Page
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import domain.model.Location
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LocationsList(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<LocationsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LocationsList(
        modifier = modifier,
        uiState = uiState,
    )
}

@Composable
private fun LocationsList(
    modifier: Modifier = Modifier,
    uiState: LocationsListUiState,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        for (page in uiState.pages) {
            when (page) {
                is Page.Placeholder -> item {
                    LocationPagePlaceholder(
                        modifier = Modifier.fillMaxWidth(),
                        page = page,
                    )
                }

                is Page.Error -> item {
                    LocationPageError(
                        modifier = Modifier.fillMaxWidth(),
                        page = page,
                    )
                }

                is Page.Success<Location> -> items(page.items, key = Location::id) { location ->
                    LocationItem(
                        modifier = Modifier.fillMaxWidth(),
                        location = location,
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun LocationPagePlaceholder(
    modifier: Modifier = Modifier,
    page: Page.Placeholder,
) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .onVisibilityChanged { isVisible ->
                if (isVisible) page.load()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.body2,
            text = "Placeholder",
        )
        IconButton(
            onClick = page::load,
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Load"
            )
        }
    }
}

@Composable
private fun LocationPageError(
    modifier: Modifier = Modifier,
    page: Page.Error,
) {
    Row(
        modifier = modifier
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.body2,
            text = page.reason.message ?: "Unknown Error"
        )
        IconButton(
            onClick = page::retry,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retry"
            )
        }
    }
}

@Composable
private fun LocationItem(
    modifier: Modifier = Modifier,
    location: Location,
) {
    Text(
        modifier = modifier
            .padding(8.dp),
        text = buildString {
            appendLine("id = ${location.id}")
            appendLine("type = ${location.type}")
            appendLine("name = ${location.name}")
            append("dimension = ${location.dimension}")
        },
    )
}
