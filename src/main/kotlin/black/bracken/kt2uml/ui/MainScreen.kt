package black.bracken.kt2uml.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun MainScreen() {
  var input by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.padding(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxHeight(),
    ) {
      OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = {
          Text(text = "Kotlin Code")
        },
        textStyle = TextStyle.Default.copy(
          fontFamily = Res.Font.JetBrainsMono,
        ),
        modifier = Modifier.fillMaxHeight().weight(1f),
      )

      Spacer(modifier = Modifier.width(16.dp))

      OutlinedTextField(
        value = input.ifEmpty { " " },
        onValueChange = { /* no-op */ },
        readOnly = true,
        label = {
          Text(text = "Generated PlantUML")
        },
        textStyle = TextStyle.Default.copy(
          fontFamily = Res.Font.JetBrainsMono,
        ),
        modifier = Modifier.fillMaxHeight().weight(1f),
      )
    }
  }
}

@Preview
@Composable
private fun MainScreenPreview() {
  MaterialTheme {
    MainScreen()
  }
}
