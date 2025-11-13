package com.vjaykrsna.nanoai.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ViewModelStateRule(config: Config) : Rule(config) {
  override val issue: Issue =
    Issue(
      id = "ViewModelSingleState",
      severity = Severity.CodeSmell,
      description =
        "ViewModels must expose a single state stream via ViewModelStateHost.state. " +
          "Avoid additional public Flow/LiveData properties or functions.",
      debt = Debt.TWENTY_MINS,
    )

  override fun visitClass(klass: KtClass) {
    if (!klass.isStateHostViewModel()) return
    val packageName = klass.containingKtFile.packageFqName.asString()
    if (targetedPackages.none { prefix -> packageName.startsWith(prefix) }) return
    super.visitClass(klass)

    val propertyViolations =
      klass.getBody()?.properties.orEmpty().filter { property ->
        property.isTopLevelMemberOf(klass) && property.exposesAdditionalFlow()
      }

    propertyViolations.forEach { property ->
      report(CodeSmell(issue, Entity.from(property), propertyViolationMessage(property)))
    }

    val functionViolations =
      klass.getBody()?.functions.orEmpty().filter { function ->
        function.isTopLevelMemberOf(klass) && function.exposesFlowReturnType()
      }

    functionViolations.forEach { function ->
      report(CodeSmell(issue, Entity.from(function), functionViolationMessage(function)))
    }
  }

  private fun KtClass.isStateHostViewModel(): Boolean =
    superTypeListEntries
      .mapNotNull { entry -> entry.typeReference?.text }
      .any { typeName -> typeName.contains("ViewModelStateHost") }

  private fun KtProperty.isTopLevelMemberOf(klass: KtClass): Boolean {
    if (isLocal) return false
    if (hasModifier(KtTokens.PRIVATE_KEYWORD)) return false
    val containingClass = getStrictParentOfType<KtClassOrObject>()
    if (containingClass != klass) return false
    val propertyName = name ?: return false
    if (propertyName in allowedPropertyNames) return false
    return true
  }

  private fun KtNamedFunction.isTopLevelMemberOf(klass: KtClass): Boolean {
    if (hasModifier(KtTokens.PRIVATE_KEYWORD)) return false
    val containingClass = getStrictParentOfType<KtClassOrObject>()
    if (containingClass != klass) return false
    return true
  }

  private fun KtProperty.exposesAdditionalFlow(): Boolean {
    val typeText = typeReference?.text?.normalized()
    if (typeText != null && flowTypeIndicators.any { indicator -> typeText.contains(indicator) }) {
      return true
    }
    val initializerText = initializer?.text?.normalized() ?: return false
    return flowExpressionIndicators.any { indicator -> initializerText.contains(indicator) }
  }

  private fun KtNamedFunction.exposesFlowReturnType(): Boolean {
    val typeText = typeReference?.text?.normalized() ?: return false
    return flowTypeIndicators.any { indicator -> typeText.contains(indicator) }
  }

  private fun propertyViolationMessage(property: KtProperty): String {
    val name = property.name ?: "property"
    return "ViewModels should expose a single state stream. '$name' leaks an additional Flow/LiveData output."
  }

  private fun functionViolationMessage(function: KtNamedFunction): String {
    val name = function.name ?: "function"
    return "ViewModels should expose a single state stream. '$name' returns an additional Flow/LiveData output."
  }

  private fun String.normalized(): String = replace("\\s".toRegex(), "")

  private val allowedPropertyNames = setOf("state", "events")
  private val targetedPackages =
    listOf(
      "com.vjaykrsna.nanoai.feature.chat",
      "com.vjaykrsna.nanoai.feature.library",
      "com.vjaykrsna.nanoai.feature.settings",
    )

  private val flowTypeIndicators =
    listOf(
      "Flow",
      "StateFlow",
      "SharedFlow",
      "LiveData",
      "MutableStateFlow",
      "MutableSharedFlow",
      "Channel",
    )

  private val flowExpressionIndicators =
    listOf(
      "MutableStateFlow",
      "MutableSharedFlow",
      "stateIn(",
      "shareIn(",
      "flowOf(",
      "channelFlow(",
      "callbackFlow(",
    )
}
