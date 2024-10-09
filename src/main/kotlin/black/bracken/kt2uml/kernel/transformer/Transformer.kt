package black.bracken.kt2uml.kernel.transformer

import black.bracken.kt2uml.kernel.FunctionParameter
import black.bracken.kt2uml.kernel.Type
import black.bracken.kt2uml.kernel.UmlTarget
import black.bracken.kt2uml.kernel.Visibility
import black.bracken.kt2uml.util.Kt2umlLogger.withWarn
import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.DefaultAstNode
import kotlinx.ast.common.ast.DefaultAstTerminal
import kotlinx.ast.common.ast.rawAstOrNull
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.grammar.kotlin.common.KotlinGrammarParserType
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser
import org.antlr.v4.kotlinruntime.misc.ParseCancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data object Transformer {

  suspend fun generateUmlTarget(code: String): List<UmlTarget>? {
    return try {
      val src = AstSource.String("description", code)
      val rootAst = KotlinGrammarAntlrKotlinParser.parse(src, KotlinGrammarParserType.kotlinFile)

      suspendCoroutine { cont ->
        rootAst.summary(false)
          .onSuccess { ast ->
            cont.resume(generateUmlTargets(ast.filterIsInstance<KlassDeclaration>()))
          }
          .onFailure { cont.resume(null) }
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun generateUmlTargets(klassDeclarations: List<KlassDeclaration>): List<UmlTarget> {
    return klassDeclarations.mapNotNull { klassDeclaration ->
      when (klassDeclaration.keyword) {
        "fun" -> generateUmlTargetForFunction(klassDeclaration)
        else -> null
      }
    }
  }

  private fun generateUmlTargetForFunction(klassDeclaration: KlassDeclaration): UmlTarget.Function? {
    val functionName =
      klassDeclaration.identifier?.identifier ?: return null.withWarn("name is null: ${klassDeclaration.identifier}")
    val annotationNames = klassDeclaration.annotations.mapNotNull { it.identifier.firstOrNull()?.rawName }
    val params = klassDeclaration.parameter.mapNotNull { decl ->
      val name = decl.identifier?.rawName ?: return@mapNotNull null
      val type = decl.asDefaultAstNode()
        ?.findChild("parameter")
        ?.findChild("type")
        ?.let { transformType(it) }
        ?: return@mapNotNull null

      FunctionParameter.TypeAndName(
        name = name,
        type = type,
      )
    }
    val returnType = klassDeclaration.asDefaultAstNode()?.findChild("type")?.let { transformType(it) } ?: Type.UNIT

    val modifiers = klassDeclaration.modifiers.map { it.modifier }
    val visibility = when {
      "private" in modifiers -> Visibility.PRIVATE
      "protected" in modifiers -> Visibility.PROTECTED
      "internal" in modifiers -> Visibility.INTERNAL
      "public" in modifiers -> Visibility.PUBLIC
      else -> Visibility.UNSPECIFIED
    }

    return UmlTarget.Function(
      name = functionName,
      annotationNames = annotationNames,
      params = params,
      returnType = returnType,
      visibility = visibility,
    )
  }

  private fun transformType(typeAst: DefaultAstNode): Type? {
    // TODO: function type
    return typeAst
      .findChild("typeReference")
      ?.findChild("userType")
      ?.findChild("simpleUserType")
      ?.findChild("simpleIdentifier")
      ?.findTerminal("Identifier")
      ?.let { Type.Reference(it.text) }
  }

  private fun KlassDeclaration.asDefaultAstNode(): DefaultAstNode? = rawAstOrNull()?.ast as? DefaultAstNode

  suspend fun genUml(code: String): String {
    return try {
      val src = AstSource.String("description", code)
      val ast = KotlinGrammarAntlrKotlinParser.parse(src, KotlinGrammarParserType.kotlinFile)

      return suspendCoroutine { cont ->
        ast.summary(false)
          .onSuccess { ast ->
            val computed = ast
              .filterIsInstance<KlassDeclaration>()
              .joinToString { genUmlForFunction(it) ?: "null" }

            cont.resume(computed)
          }
          .onFailure { cont.resume("ERROR") }
      }
    } catch (ex: ParseCancellationException) {
      "error"
    }
  }

  private const val NO_NAME = "NoName"

  private fun genUmlForFunction(klassDeclaration: KlassDeclaration): String? {
    return when (klassDeclaration.keyword) {
      "fun" -> {
        val name = klassDeclaration.identifier?.identifier ?: NO_NAME
        val annotations = klassDeclaration.annotations
        val params = klassDeclaration.parameter
        val returnType = klassDeclaration.type.firstOrNull()?.rawName ?: "Unit"

        val modifiers = klassDeclaration.modifiers.map { it.modifier }
        val visibilitySymbol = when {
          "private" in modifiers -> "-"
          "protected" in modifiers -> "#"
          "internal" in modifiers -> "~"
          else -> "+" // public or default
        }
        // TODO: support 拡張関数

        """
          interface $name <<${annotations.joinToString(separator = " ") { "@${it.identifier.firstOrNull()?.rawName}" }} top-level function>> {
            $visibilitySymbol invoke: (${
          params.joinToString(separator = ", ") { "${it.identifier?.rawName ?: NO_NAME}: ${parameterToString(it)}" }
        }) -> $returnType
          }
        """.trimIndent()
      }

      else -> null
    }
  }

  // 型のdeclaration T から 文字列 "T" を得る f
  private fun parameterToString(dec: KlassDeclaration): String {
    // primitive or ref type
    // TODO: A<() -> T>のASTが取れない
    val rawName = dec.type.firstOrNull()?.rawName
    if (rawName != null) return rawName

    // function type
    val returnType = dec.type.firstOrNull()?.rawName ?: "Unit"
    val suspendPrefix = if (dec.modifiers.any { it.modifier == "suspend" }) {
      "suspend "
    } else {
      ""
    }

    // ラムダ式の引数のAST(functionTypeParametersまで)
    val functionTypeParametersAst = (dec.rawAstOrNull()?.ast as? DefaultAstNode)
      ?.findChild("parameter")
      ?.findChild("type")
      ?.findChild("functionType")
      ?.findChild("functionTypeParameters")
      ?: throw IllegalStateException("No functionTypeParameters found")


    // 📝 param: (x: (y: Y) -> Unit) -> String
    // parameter -> type -> functionType((x: (y: Y) -> Unit) -> functionTypeParameters -> parameter -> type -> functionType((y: Y) -> Unit) -> functionTypeParameters -> parameter -> type -> typeReference(Y)
    // TODO typeAstからFunctionTypeを再帰的に得て、関数型の引数と返り値を解釈する

    val param = when {
      // 引数が無名のとき
      functionTypeParametersAst.hasChild("type") -> {
        functionTypeParametersAst.findChild("type")
          ?.findChild("typeReference")
          ?.findChild("userType")
          ?.findChild("simpleUserType")
          ?.findChild("simpleIdentifier")
          ?.findTerminal("Identifier")
          ?.text
      }

      // 引数に名前が付けられているとき
      functionTypeParametersAst.hasChild("parameter") -> {
        val parameterNode = functionTypeParametersAst.findChild("parameter")

        val name = parameterNode
          ?.findChild("simpleIdentifier")
          ?.findTerminal("Identifier")
          ?.text
        val type = parameterNode
          ?.findChild("type")
          ?.findChild("typeReference")
          ?.findChild("userType")
          ?.findChild("simpleUserType")
          ?.findChild("simpleIdentifier")
          ?.findTerminal("Identifier")
          ?.text

        "$name: $type"
      }

      else -> throw IllegalStateException("No matched value")
    }

    return "$suspendPrefix(${dec.parameter.joinToString { parameterToString(it) }}) -> $returnType"
  }

  private fun DefaultAstNode.hasChild(description: String): Boolean {
    return children.any { it.description == description }
  }

  private fun DefaultAstNode.findChild(description: String): DefaultAstNode? {
    return children.find { it.description == description } as? DefaultAstNode
  }

  private fun DefaultAstNode.findTerminal(description: String): DefaultAstTerminal? {
    return children.find { it.description == description } as? DefaultAstTerminal
  }

}
