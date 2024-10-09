package black.bracken.kt2uml.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import black.bracken.kt2uml.ui.Res
import kotlinx.coroutines.launch

@Composable
fun MainScreenCoordinator() {
  val scope = rememberCoroutineScope()
  val mainStateHolder = remember { MainStateHolder(scope) }
  val uiState by mainStateHolder.uiState.collectAsState()

  MainScreen(
    uiState = uiState,
    onChangeCode = mainStateHolder::changeCode,
  )
}

@Composable
internal fun MainScreen(
  uiState: MainUiState,
  onChangeCode: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current
  val snackbarHostState = remember { SnackbarHostState() }

  Scaffold(
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    modifier = Modifier.fillMaxSize(),
  ) { paddingValues ->
    Row(
      modifier = Modifier
        .padding(paddingValues)
        .padding(8.dp)
        .fillMaxHeight(),
    ) {
      OutlinedTextField(
        value = uiState.code,
        onValueChange = onChangeCode,
        label = {
          Text(text = "Kotlin Code")
        },
        textStyle = TextStyle.Default.copy(
          fontFamily = Res.Font.JetBrainsMono,
        ),
        modifier = Modifier
          .fillMaxHeight()
          .weight(1f),
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(
        modifier = Modifier.weight(1f)
      ) {
        OutlinedTextField(
          value = uiState.shownUml.ifEmpty { " " },
          onValueChange = { /* no-op */ },
          readOnly = true,
          label = {
            Text(text = "Generated PlantUML")
          },
          textStyle = TextStyle.Default.copy(
            fontFamily = Res.Font.JetBrainsMono,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.align(Alignment.End)
        ) {
          Icon(
            imageVector = when (uiState.generateState) {
              GenerateState.Generated -> Icons.Filled.Done
              GenerateState.Generating -> Icons.Filled.Refresh
              GenerateState.Invalid -> Icons.Filled.Warning
            },
            contentDescription = null,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              scope.launch {
                clipboardManager.setText(AnnotatedString(uiState.shownUml))
                snackbarHostState.showSnackbar("Copied UML")
              }
            },
          ) {
            Text(text = "Copy")
          }
        }
      }
    }
  }
}
