package black.bracken.kt2uml.kernel

sealed interface UmlTarget {

  data class Function(
    val name: String,
    val annotationNames: List<String>,
    val params: List<FunctionParameter.TypeAndName>,
    val returnType: Type,
    val visibility: Visibility,
  ) : UmlTarget

}

enum class Visibility {
  PRIVATE,
  PROTECTED,
  INTERNAL,
  PUBLIC,
  UNSPECIFIED, // NOTE: pluginなどでデフォルトの可視性をprivateにしている場合のサポート
}

sealed interface FunctionParameter {
  data class JustType(
    override val type: Type,
  ) : FunctionParameter

  data class TypeAndName(
    override val type: Type,
    val name: String,
  ) : FunctionParameter

  val type: Type
}

sealed interface Type {
  data class Reference(val typeName: String) : Type
  data class Function(val params: List<FunctionParameter>, val returnType: Type) : Type

  companion object {
    val UNIT = Reference(typeName = "Unit")
  }
}
