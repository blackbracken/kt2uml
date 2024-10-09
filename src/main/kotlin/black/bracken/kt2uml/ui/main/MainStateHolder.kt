package black.bracken.kt2uml.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import black.bracken.kt2uml.kernel.UmlTarget
import black.bracken.kt2uml.kernel.printer.PlantUmlPrinter
import black.bracken.kt2uml.kernel.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainStateHolder(coroutineScope: CoroutineScope) {

  private val code = MutableStateFlow("")

  val uiState = coroutineScope.launchMolecule(RecompositionMode.Immediate) {
    presenter()
  }

  fun changeCode(input: String) {
    code.update { input }
  }

  @Composable
  fun presenter(): MainUiState {
    val code by code.collectAsState()
    var shownUml by remember { mutableStateOf("") }
    var generateState by remember { mutableStateOf(GenerateState.Generated) }

    LaunchedEffect(code) {
      generateState = GenerateState.Generating

      val printed = Transformer.transformUmlTarget(code)
        ?.joinToString("\n\n") {
          when (it) {
            is UmlTarget.Function -> PlantUmlPrinter.printFunction(it)
          }
        }

      if (printed != null) {
        shownUml = printed
        generateState = GenerateState.Generated
      } else {
        generateState = GenerateState.Invalid
      }
    }

    return MainUiState(
      code = code,
      shownUml = shownUml,
      generateState = generateState,
    )
  }

}
