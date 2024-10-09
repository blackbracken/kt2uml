package black.bracken.kt2uml.ui.main

data class MainUiState(
  val code: String,
  val shownUml: String,
  val generateState: GenerateState,
) {
}

enum class GenerateState {
  Generated,
  Generating,
  Invalid,
}
