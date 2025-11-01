package com.vjaykrsna.nanoai.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

/**
 * Custom Detekt rules for architecture compliance.
 */

/**
 * Detects direct repository injection in ViewModel classes.
 *
 * ViewModels should only inject UseCases, not repositories directly.
 */
class NoRepositoryInjectionInViewModels(config: Config) : Rule(config) {

  override val issue = Issue(
    id = "NoRepositoryInjectionInViewModels",
    severity = Severity.Error,
    description = "ViewModels should not inject repositories directly. Use UseCases instead.",
    debt = Debt.TEN_MINS,
  )

  override fun visitKtFile(file: KtFile) {
    super.visitKtFile(file)

    file.children.forEach { element ->
      when (element) {
        is KtClass -> checkViewModelClass(element)
        else -> Unit
      }
    }
  }

  private fun checkViewModelClass(ktClass: KtClass) {
    // Check if class is annotated with @HiltViewModel
    val hasHiltViewModelAnnotation = ktClass.annotationEntries.any { annotation ->
      annotation.text.contains("HiltViewModel")
    }

    if (!hasHiltViewModelAnnotation) return

    // Check constructor parameters for repository injections
    val primaryConstructor = ktClass.primaryConstructor
    primaryConstructor?.valueParameters?.forEach { parameter ->
      checkParameterForRepository(parameter, ktClass)
    }
  }

  private fun checkParameterForRepository(parameter: KtParameter, ktClass: KtClass) {
    val typeReference = parameter.typeReference ?: return
    val typeText = typeReference.text

    // Check if parameter type ends with "Repository"
    if (typeText.endsWith("Repository") || typeText.contains("Repository<")) {
      report(
        CodeSmell(
          issue = issue,
          entity = Entity.from(parameter),
          message = "ViewModel '${ktClass.name}' injects repository '${typeText}'. Use a UseCase instead.",
        ),
      )
    }
  }
}

/**
 * Detects ViewModels that inject multiple dependencies of the same type.
 *
 * This might indicate mixed architecture layers (e.g., injecting both Repository and UseCase).
 */
class UseCaseOnlyInjection(config: Config) : Rule(config) {

  override val issue = Issue(
    id = "UseCaseOnlyInjection",
    severity = Severity.Warning,
    description = "ViewModels should only inject UseCases for business logic.",
    debt = Debt.FIVE_MINS,
  )

  override fun visitKtFile(file: KtFile) {
    super.visitKtFile(file)

    file.children.forEach { element ->
      when (element) {
        is KtClass -> checkViewModelInjections(element)
        else -> Unit
      }
    }
  }

  private fun checkViewModelInjections(ktClass: KtClass) {
    val hasHiltViewModelAnnotation = ktClass.annotationEntries.any { annotation ->
      annotation.text.contains("HiltViewModel")
    }

    if (!hasHiltViewModelAnnotation) return

    val primaryConstructor = ktClass.primaryConstructor ?: return
    val parameters = primaryConstructor.valueParameters

    val injectedTypes = mutableSetOf<String>()
    val duplicateTypes = mutableSetOf<String>()

    parameters.forEach { parameter ->
      val typeReference = parameter.typeReference ?: return@forEach
      val typeText = typeReference.text

      if (injectedTypes.contains(typeText)) {
        duplicateTypes.add(typeText)
      } else {
        injectedTypes.add(typeText)
      }
    }

    if (duplicateTypes.isNotEmpty()) {
      report(
        CodeSmell(
          issue = issue,
          entity = Entity.from(ktClass),
          message = "ViewModel '${ktClass.name}' has multiple injections of the same type: ${duplicateTypes.joinToString()}. Consider consolidating into a single UseCase.",
        ),
      )
    }
  }
}
