package black.bracken.kt2uml.kernel

import kotlinx.ast.common.AstSource
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.grammar.kotlin.common.KotlinGrammarParserType
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser
import kotlinx.coroutines.runBlocking
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
