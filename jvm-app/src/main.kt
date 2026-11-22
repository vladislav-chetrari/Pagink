import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import presentation.LocationsList
import presentation.module

fun main() {
    startKoin {
        modules(module)
    }

    application {
        Window(
            title = "Rick & Morty locations",
            onCloseRequest = { exitApplication() },
        ) {
            LocationsList()
        }
    }
}
