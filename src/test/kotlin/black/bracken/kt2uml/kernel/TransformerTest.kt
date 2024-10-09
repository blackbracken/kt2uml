package black.bracken.kt2uml.kernel

import black.bracken.kt2uml.kernel.transformer.Transformer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TransformerTest {

  @Test
  fun testUnknown() = runBlocking {
    val actual = """
      UNKNOWN
    """.transformCode()

    val expected = null

    Assertions.assertEquals(expected, actual)
  }

  @Test
  fun testFunction_default() = runBlocking {
    val actual = """
      @Annotation
      protected fun f(x: Int): String {}
    """.transformCode()

    val expected = UmlTarget.Function(
      name = "f",
      annotationNames = listOf("Annotation"),
      params = listOf(
        FunctionParameter.TypeAndName(
          type = Type.Reference(typeName = "Int"),
          name = "x",
        )
      ),
      returnType = Type.Reference("String"),
      visibility = Visibility.PROTECTED
    )

    Assertions.assertEquals(listOf(expected), actual)
  }

  @Test
  fun testFunction_functionTypeParameterFunctionTypeParameter() = runBlocking {
    val actual = """
      fun f(x: ((String) -> Int) -> Unit) {}
    """.transformCode()

    val expected = UmlTarget.Function(
      name = "f",
      annotationNames = listOf(),
      params = listOf(
        FunctionParameter.TypeAndName(
          name = "x",
          type = Type.Function(
            params = listOf(
              FunctionParameter.JustType(
                type = Type.Function(
                  params = listOf(FunctionParameter.JustType(type = Type.Reference(typeName = "String"))),
                  returnType = Type.Reference(typeName = "Int"),
                )
              )
            ),
            returnType = Type.UNIT,
          ),
        ),
      ),
      returnType = Type.UNIT,
      visibility = Visibility.UNSPECIFIED,
    )

    Assertions.assertEquals(listOf(expected), actual)
  }

  private suspend fun String.transformCode(): List<UmlTarget>? {
    return Transformer.generateUmlTarget(this.trimIndent())
  }

}
