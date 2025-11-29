package com.vjaykrsna.nanoai.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile

class CleanArchitectureRule(config: Config) : Rule(config) {
  override val issue: Issue =
    Issue(
      id = "CleanArchitectureLayering",
      severity = Severity.CodeSmell,
      description =
        "Enforces Clean Architecture boundaries so UI features cannot depend directly on data layers, " +
          "domain models cannot import data entities, and core layers never reference feature code.",
      debt = io.gitlab.arturbosch.detekt.api.Debt.TWENTY_MINS,
    )

  override fun visitKtFile(file: KtFile) {
    val packageName = file.packageFqName.asString()
    val imports = file.importDirectives

    imports
      .mapNotNull { directive ->
        val importPath = directive.importPath?.pathStr ?: return@mapNotNull null
        val entity = Entity.from(directive)
        when {
          packageName.contains(".feature.") && importPath.contains(".core.data.") ->
            CodeSmell(issue, entity, featureDependsOnDataMessage(packageName, importPath))
          packageName.startsWith("com.vjaykrsna.nanoai.core") && importPath.contains(".feature.") ->
            CodeSmell(issue, entity, coreDependsOnFeatureMessage(packageName, importPath))
          // Domain models should not import data entities (mappers belong in data layer)
          packageName.contains(".domain.model") && importPath.contains(".data.db.entities") ->
            CodeSmell(issue, entity, domainImportsDataEntityMessage(packageName, importPath))
          else -> null
        }
      }
      .forEach(::report)
  }

  private fun featureDependsOnDataMessage(packageName: String, importPath: String): String =
    "Feature module '$packageName' should avoid importing data layer symbol '$importPath'. " +
      "Route interactions through domain use cases instead."

  private fun coreDependsOnFeatureMessage(packageName: String, importPath: String): String =
    "Core layer '$packageName' must not reference feature package '$importPath'. " +
      "Move shared functionality into a core module."

  private fun domainImportsDataEntityMessage(packageName: String, importPath: String): String =
    "Domain model '$packageName' should not import data entity '$importPath'. " +
      "Move mapping functions (toDomain/toEntity) to the data layer mappers."
}
