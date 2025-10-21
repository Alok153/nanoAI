package com.vjaykrsna.nanoai.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Custom Detekt rules for UI quality and accessibility compliance.
 */

/**
 * Detects hardcoded strings in UI composables that should be extracted to string resources.
 */
class HardcodedStringInComposable(config: Config) : Rule(config) {

  override val issue = Issue(
    id = "HardcodedStringInComposable",
    severity = Severity.Warning,
    description = "Hardcoded strings in composables should be extracted to string resources for localization",
    debt = Debt.FIVE_MINS,
  )

  private val composableAnnotation = "Composable"
  private val allowedComposableFunctions = setOf(
    "Text",
    "Button",
    "OutlinedButton",
    "TextButton",
    "FloatingActionButton",
    "IconButton",
    "Card",
    "TopAppBar",
    "BottomAppBar",
    "Scaffold",
    "ModalDrawerSheet",
    "NavigationDrawerItem",
    "Tab",
    "Chip",
    "Badge",
    "Snackbar",
    "AlertDialog",
    "DropdownMenuItem",
    "ListItem",
    "RadioButton",
    "Checkbox",
    "Switch",
    "Slider",
    "TextField",
    "OutlinedTextField",
    "BasicTextField",
  )

  override fun visitKtFile(file: KtFile) {
    super.visitKtFile(file)

    // Only check UI-related files
    if (!isUIFile(file)) return

    file.children.forEach { element ->
      when (element) {
        is KtCallExpression -> checkComposableCall(element)
        else -> Unit
      }
    }
  }

  private fun isUIFile(file: KtFile): Boolean {
    val path = file.virtualFilePath
    return path.contains("/ui/") ||
           path.contains("/feature/") && (path.contains("/ui/") || path.endsWith("Screen.kt")) ||
           path.contains("Composable.kt") ||
           path.contains("Component.kt")
  }

  private fun checkComposableCall(call: KtCallExpression) {
    val functionName = call.calleeExpression?.text ?: return

    if (functionName in allowedComposableFunctions) {
      call.valueArguments.forEach { arg ->
        checkArgumentForHardcodedString(arg, functionName)
      }
    }
  }

  private fun checkArgumentForHardcodedString(arg: KtValueArgument, functionName: String) {
    val expression = arg.getArgumentExpression() ?: return

    if (expression is KtStringTemplateExpression) {
      // Check if it's a simple string literal (not a template with variables)
      val entries = expression.entries
      if (entries.size == 1 && entries[0] is KtLiteralStringTemplateEntry) {
        val stringValue = (entries[0] as KtLiteralStringTemplateEntry).text

        // Skip very short strings, empty strings, or strings that look like keys
        if (stringValue.length > 2 &&
            !stringValue.contains('$') &&
            !looksLikeResourceKey(stringValue)) {

          report(
            CodeSmell(
              issue = issue,
              entity = Entity.from(arg),
              message = "Hardcoded string '$stringValue' in $functionName should be extracted to string resources",
            ),
          )
        }
      }
    }
  }

  private fun looksLikeResourceKey(string: String): Boolean {
    // Check if it looks like a resource key (snake_case, camelCase, or dotted notation)
    return string.contains('_') ||
           string.contains('.') ||
           string.matches(Regex("[a-z]+[A-Z][a-zA-Z]*"))
  }
}

/**
 * Detects missing contentDescription for accessibility in Image and Icon composables.
 */
class MissingContentDescription(config: Config) : Rule(config) {

  override val issue = Issue(
    id = "MissingContentDescription",
    severity = Severity.Warning,
    description = "Images and icons should have contentDescription for accessibility",
    debt = Debt.FIVE_MINS,
  )

  private val imageFunctions = setOf("Image", "Icon", "AsyncImage", "CoilImage")

  override fun visitKtFile(file: KtFile) {
    super.visitKtFile(file)

    if (!isUIFile(file)) return

    file.children.forEach { element ->
      when (element) {
        is KtCallExpression -> checkImageCall(element)
        else -> Unit
      }
    }
  }

  private fun isUIFile(file: KtFile): Boolean {
    val path = file.virtualFilePath
    return path.contains("/ui/") ||
           path.contains("/feature/") ||
           path.contains("Composable.kt") ||
           path.contains("Component.kt")
  }

  private fun checkImageCall(call: KtCallExpression) {
    val functionName = call.calleeExpression?.text ?: return

    if (functionName in imageFunctions) {
      // Check if contentDescription parameter is present
      val hasContentDescription = call.valueArguments.any { arg ->
        arg.getArgumentName()?.text == "contentDescription"
      }

      if (!hasContentDescription) {
        report(
          CodeSmell(
            issue = issue,
            entity = Entity.from(call),
            message = "$functionName is missing contentDescription for accessibility",
          ),
        )
      }
    }
  }
}

/**
 * Detects composables that might violate Material 3 spacing guidelines.
 */
class NonMaterialSpacing(config: Config) : Rule(config) {

  override val issue = Issue(
    id = "NonMaterialSpacing",
    severity = Severity.Info,
    description = "Consider using MaterialTheme.spacing for consistent spacing",
    debt = Debt.TWO_MINS,
  )

  private val spacingFunctions = setOf("padding", "Modifier.padding")
  private val materialSpacingPattern = Regex("\\d+\\.dp|\\bspacing\\.")

  override fun visitKtFile(file: KtFile) {
    super.visitKtFile(file)

    if (!isUIFile(file)) return

    file.children.forEach { element ->
      when (element) {
        is KtCallExpression -> checkSpacingCall(element)
        else -> Unit
      }
    }
  }

  private fun isUIFile(file: KtFile): Boolean {
    val path = file.virtualFilePath
    return path.contains("/ui/") ||
           path.contains("/feature/") ||
           path.contains("Composable.kt") ||
           path.contains("Component.kt")
  }

  private fun checkSpacingCall(call: KtCallExpression) {
    val functionName = call.calleeExpression?.text ?: return

    if (functionName in spacingFunctions || functionName.endsWith(".padding")) {
      call.valueArguments.forEach { arg ->
        val expression = arg.getArgumentExpression()
        if (expression is KtStringTemplateExpression) {
          val text = expression.text
          // Check for hardcoded dp values that don't use Material spacing
          if (text.matches(Regex(".*\\d+\\.dp.*")) && !text.contains("spacing")) {
            report(
              CodeSmell(
                issue = issue,
                entity = Entity.from(arg),
                message = "Consider using MaterialTheme.spacing instead of hardcoded dp values",
              ),
            )
          }
        }
      }
    }
  }
}
