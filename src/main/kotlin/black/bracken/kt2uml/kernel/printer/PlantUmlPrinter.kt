package black.bracken.kt2uml.kernel.printer

import black.bracken.kt2uml.kernel.FunctionParameter
import black.bracken.kt2uml.kernel.Type
import black.bracken.kt2uml.kernel.UmlTarget
import black.bracken.kt2uml.kernel.Visibility

data object PlantUmlPrinter : UmlPrinter {

  override fun printFunction(target: UmlTarget.Function): String {
    val annotationsText = if (target.annotationNames.isNotEmpty()) {
      target.annotationNames.joinToString(separator = " ") { "@$it" } + " "
    } else {
      ""
    }

    val suspendText = if (target.isSuspend) {
      " <<suspend>>"
    } else {
      ""
    }

    val visibilitySymbol = when (target.visibility) {
      Visibility.PRIVATE -> "-"
      Visibility.PROTECTED -> "#"
      Visibility.INTERNAL -> "~"
      Visibility.PUBLIC -> "+"
      Visibility.UNSPECIFIED -> "+" // default visibility
    }

    return """
    interface ${target.name} <<${annotationsText}top-level function>> {
      $visibilitySymbol invoke: (${target.params.joinToString { "${it.name}: ${typeToString(it.type)}" }}) -> ${
      typeToString(
        target.returnType
      )
    }${suspendText}
    }
    """.trimIndent()
  }

  private fun typeToString(type: Type): String {
    return when (type) {
      is Type.Reference -> {
        type.typeName
      }

      is Type.Function -> {
        "(${
          type.params.joinToString { param ->
            when (param) {
              is FunctionParameter.TypeAndName -> "${param.name}: ${typeToString(param.type)}"
              is FunctionParameter.JustType -> typeToString(param.type)
            }
          }
        }) -> ${typeToString(type.returnType)}"
      }
    }
  }

}
