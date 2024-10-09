package black.bracken.kt2uml.kernel

import black.bracken.kt2uml.kernel.transformer.Transformer
import kotlinx.ast.common.AstSource
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.grammar.kotlin.common.KotlinGrammarParserType
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TransformerTest {

  @Test
  fun f() = runBlocking {
    val code = """
      fun f(x: (a: A, b: B, C) -> Unit): X = TODO()
    """.trimIndent()
    println(parse(code))
  }

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

  private suspend fun String.transformCode(): List<UmlTarget>? {
    return Transformer.generateUmlTarget(this.trimIndent())
  }

  private suspend fun parse(code: String): List<KlassDeclaration> {
    val src = AstSource.String("description", code)
    val parsed = KotlinGrammarAntlrKotlinParser.parse(src, KotlinGrammarParserType.kotlinFile)

    return suspendCoroutine { cont ->
      parsed
        .summary(false)
        .onSuccess { cont.resume(it.filterIsInstance<KlassDeclaration>()) }
        .onFailure { cont.resume(emptyList()) }
    }
  }

}
