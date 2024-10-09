package black.bracken.kt2uml

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import black.bracken.kt2uml.ui.MainScreen

fun main() = application {
  Window(
    title = "kt2uml",
    onCloseRequest = ::exitApplication,
    ) {
    MainScreen()
  }
}
