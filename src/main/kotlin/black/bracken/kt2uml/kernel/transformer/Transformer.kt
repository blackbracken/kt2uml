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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data object Transformer {

  suspend fun transformUmlTarget(code: String): List<UmlTarget>? {
    return try {
      val src = AstSource.String("description", code)
      val rootAst = KotlinGrammarAntlrKotlinParser.parse(src, KotlinGrammarParserType.kotlinFile)

      suspendCoroutine { cont ->
        rootAst.summary(false)
          .onSuccess { ast ->
            cont.resume(transformUmlTargets(ast.filterIsInstance<KlassDeclaration>()))
          }
          .onFailure { cont.resume(null) }
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun transformUmlTargets(klassDeclarations: List<KlassDeclaration>): List<UmlTarget> {
    return klassDeclarations.mapNotNull { klassDeclaration ->
      when (klassDeclaration.keyword) {
        "fun" -> transformUmlTargetForFunction(klassDeclaration)
        else -> null
      }
    }
  }

  private fun transformUmlTargetForFunction(klassDeclaration: KlassDeclaration): UmlTarget.Function? {
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
    return when {
      // 参照型
      typeAst.hasChild("typeReference") -> {
        typeAst.findChild("typeReference")
          ?.findChild("userType")
          ?.findChild("simpleUserType")
          ?.findChild("simpleIdentifier")
          ?.findTerminal("Identifier")
          ?.let { Type.Reference(it.text) }
      }

      // 関数型
      typeAst.hasChild("functionType") -> {
        val functionTypeAst = typeAst.findChild("functionType") ?: return null
        val functionTypeParametersAst = functionTypeAst.findChild("functionTypeParameters") ?: return null

        val returnType = functionTypeAst.findChild("type")?.let { transformType(it) } ?: return null

        val params = functionTypeParametersAst.findChildren("type", "parameter")
          .mapNotNull { ast ->
            when (ast.description) {
              // 引数が無名のとき
              "type" -> {
                val type = transformType(ast) ?: return@mapNotNull null

                FunctionParameter.JustType(
                  type = type,
                )
              }

              // 引数に名前が付けられているとき
              "parameter" -> {
                val paramName = ast.findChild("simpleIdentifier")
                  ?.findTerminal("Identifier")
                  ?.text
                  ?: return@mapNotNull null
                val type = ast.findChild("type")
                  ?.let { transformType(it) }
                  ?: return@mapNotNull null

                FunctionParameter.TypeAndName(
                  type = type,
                  name = paramName,
                )
              }

              else -> return@mapNotNull null.withWarn("function type parameter could not be interpretted")
            }
          }

        Type.Function(
          params = params,
          returnType = returnType,
        )
      }

      else -> null
    }
  }

  private fun KlassDeclaration.asDefaultAstNode(): DefaultAstNode? = rawAstOrNull()?.ast as? DefaultAstNode

  private fun DefaultAstNode.hasChild(description: String): Boolean {
    return children.any { it.description == description }
  }

  private fun DefaultAstNode.findChild(description: String): DefaultAstNode? {
    return children.find { it.description == description } as? DefaultAstNode
  }

  private fun DefaultAstNode.findChildren(vararg descriptions: String): List<DefaultAstNode> {
    return children.filterIsInstance<DefaultAstNode>().filter { it.description in descriptions }
  }

  private fun DefaultAstNode.findTerminal(description: String): DefaultAstTerminal? {
    return children.find { it.description == description } as? DefaultAstTerminal
  }

}
