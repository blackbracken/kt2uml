package black.bracken.kt2uml.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

data object Res {
  data object Font {
    val JetBrainsMono = FontFamily(
      Font(
        resource = "JetBrainsMono-Medium.ttf",
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
      )
    )
  }
}