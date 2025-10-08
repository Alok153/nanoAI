package com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoRadii

@Composable
fun NanoInputField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  placeholder: String? = null,
  supportingText: String? = null,
  enabled: Boolean = true,
  singleLine: Boolean = true,
  minLines: Int = 1,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  isError: Boolean = false,
  readOnly: Boolean = false,
) {
  val semanticsModifier =
    modifier.semantics { contentDescription = label ?: placeholder ?: "Input field" }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = semanticsModifier,
    enabled = enabled,
    singleLine = singleLine,
    minLines = minLines,
    maxLines = maxLines,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    isError = isError,
    label = label?.let { providedLabel -> { Text(providedLabel) } },
    placeholder = placeholder?.let { hint -> { Text(hint) } },
    supportingText = supportingText?.let { helper -> { Text(helper) } },
    shape = RoundedCornerShape(NanoRadii.medium),
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    readOnly = readOnly,
  )
}
