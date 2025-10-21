# UI Components Guide: Material Design 3 Compliance

This guide documents the UI component patterns and Material Design 3 compliance standards used throughout the nanoAI application. All components follow accessibility-first design with comprehensive testing coverage.

## Table of Contents

- [Core Principles](#core-principles)
- [Component Patterns](#component-patterns)
- [Accessibility Standards](#accessibility-standards)
- [Material Design 3 Compliance](#material-design-3-compliance)
- [Testing Guidelines](#testing-guidelines)
- [Performance Considerations](#performance-considerations)

## Core Principles

### 1. Accessibility First
- **Touch Targets**: Minimum 48dp for all interactive elements (WCAG AA compliance)
- **Color Contrast**: ≥4.5:1 for normal text, ≥3:1 for large text
- **Content Descriptions**: All images and icons must have descriptive labels
- **Semantic Structure**: Proper heading hierarchy and navigation landmarks
- **Keyboard Navigation**: Full keyboard support with skip links

### 2. Material Design 3 Compliance
- **Semantic Tokens**: Use `MaterialTheme.colorScheme`, `typography`, `shapes`, `spacing`
- **Dynamic Color**: Support for system theme and user preferences
- **Consistent Spacing**: Use `MaterialTheme.spacing` instead of hardcoded dp values
- **Elevation Hierarchy**: Proper use of surface tints and shadow elevation

### 3. Performance Focused
- **Lazy Loading**: Use `LazyColumn` and `LazyRow` for large lists
- **State Preservation**: Implement `rememberSaveable` for scroll positions
- **Composition Optimization**: Minimize recompositions with stable keys

## Component Patterns

### Buttons and Interactive Elements

```kotlin
Button(
    onClick = { /* action */ },
    modifier = Modifier
        .minimumTouchTargetSize() // Ensures 48dp touch target
        .semantics {
            contentDescription = stringResource(R.string.action_description)
        }
) {
    Text(stringResource(R.string.button_text))
}
```

**Key Requirements**: 48dp minimum touch targets, semantic content descriptions, no hardcoded strings.

### Cards and Surfaces

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp,
        pressedElevation = 8.dp
    ),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
) {
    // Card content
}
```

**Key Requirements**: Use `CardDefaults` for elevation and colors, semantic color scheme tokens.

### Images and Icons

```kotlin
// Icon with content description
Icon(
    imageVector = Icons.Default.Star,
    contentDescription = stringResource(R.string.favorite_icon_description),
    modifier = Modifier.minimumTouchTargetSize()
)

// AsyncImage with loading states
AsyncImage(
    model = imageUrl,
    contentDescription = stringResource(R.string.profile_image_description),
    modifier = Modifier
        .size(48.dp)
        .clip(CircleShape),
    placeholder = painterResource(R.drawable.placeholder),
    error = painterResource(R.drawable.error_image)
)
```

**Key Requirements**: All images must have content descriptions, proper loading states for AsyncImage.

### Text and Typography

```kotlin
// Material 3 typography
Text(
    text = stringResource(R.string.welcome_message),
    style = MaterialTheme.typography.headlineSmall,
    color = MaterialTheme.colorScheme.onSurface
)

// Rich text with annotations
Text(
    text = buildAnnotatedString {
        append("Regular text ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("bold text")
        }
    },
    style = MaterialTheme.typography.bodyLarge
)
```

**Key Requirements**: Always use `MaterialTheme.typography`, semantic color scheme, no hardcoded font properties.

### Lists and Lazy Components

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(MaterialTheme.spacing.medium)
) {
    items(
        items = messages,
        key = { it.id },
        contentType = { it.role }
    ) { message ->
        MessageItem(
            message = message,
            modifier = Modifier
                .fillMaxWidth()
                .animateItem()
        )
    }
}
```

**Key Requirements**: Always use `key` and `contentType` parameters, `MaterialTheme.spacing` for padding, `animateItem()` for smooth animations.

### Dialogs and Modals

```kotlin
AlertDialog(
    onDismissRequest = { /* dismiss */ },
    title = {
        Text(
            text = stringResource(R.string.confirm_action_title),
            style = MaterialTheme.typography.headlineSmall
        )
    },
    text = {
        Text(stringResource(R.string.confirm_action_message))
    },
    confirmButton = {
        TextButton(onClick = { /* confirm */ }) {
            Text(stringResource(R.string.confirm))
        }
    },
    dismissButton = {
        TextButton(onClick = { /* dismiss */ }) {
            Text(stringResource(R.string.cancel))
        }
    }
)
```

**Key Requirements**: Proper semantic structure with title/text separation, dismiss handling, accessible button labels.

### Form Controls

```kotlin
// TextField with validation
OutlinedTextField(
    value = textValue,
    onValueChange = { /* update */ },
    label = { Text(stringResource(R.string.name_label)) },
    supportingText = {
        if (isError) Text(stringResource(R.string.name_error))
    },
    isError = isError,
    modifier = Modifier.fillMaxWidth(),
    keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Next
    )
)

// Switch with semantic properties
Switch(
    checked = isEnabled,
    onCheckedChange = { /* toggle */ },
    modifier = Modifier.semantics {
        contentDescription = stringResource(
            if (isEnabled) R.string.setting_enabled_desc
            else R.string.setting_disabled_desc
        )
    }
)
```

**Key Requirements**: Proper labeling, error states, semantic content descriptions, appropriate keyboard options.

## Accessibility Standards

### Touch Targets
- **Minimum Size**: 48dp x 48dp for all interactive elements
- **Padding**: Include adequate spacing around touch targets
- **Adjacent Elements**: Ensure 8dp minimum separation between touch targets

### Content Descriptions
```kotlin
// Provide context-specific descriptions
IconButton(onClick = { /* action */ }) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(R.string.delete_item, itemName)
    )
}
```

### Focus Management
```kotlin
// Manage focus for dynamic content
val focusRequester = remember { FocusRequester() }

LaunchedEffect(showDialog) {
    if (showDialog) {
        focusRequester.requestFocus()
    }
}

AlertDialog(
    // ...
) {
    TextField(
        // ...
        modifier = Modifier.focusRequester(focusRequester)
    )
}
```

### Semantic Properties
```kotlin
// Mark headings for screen readers
Text(
    text = stringResource(R.string.section_title),
    style = MaterialTheme.typography.headlineSmall,
    modifier = Modifier.semantics {
        heading()
        contentDescription = stringResource(R.string.section_title_description)
    }
)
```

## Material Design 3 Compliance

### Color System
```kotlin
// Use semantic color tokens
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
) {
    // Content uses appropriate on-surface colors
    Text(
        text = "Content",
        color = MaterialTheme.colorScheme.onSurface
    )
}
```

### Spacing System
```kotlin
// Use Material spacing tokens
Column(
    modifier = Modifier.padding(MaterialTheme.spacing.medium),
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
) {
    // Consistent spacing throughout
}
```

### Typography Scale
```kotlin
// Use complete typography scale
Column {
    Text(
        text = "Headline",
        style = MaterialTheme.typography.headlineLarge
    )
    Text(
        text = "Title",
        style = MaterialTheme.typography.titleLarge
    )
    Text(
        text = "Body",
        style = MaterialTheme.typography.bodyLarge
    )
}
```

## Testing Guidelines

### Accessibility Testing
```kotlin
@Test
fun component_meets_accessibility_standards() {
    // Test touch targets, content descriptions, semantic structure
    composeTestRule.onNode(hasTestTag("interactive_element"))
        .assertMinimumTouchTarget()
        .assert(hasContentDescription())
}
```

### Material Design Testing
```kotlin
@Test
fun component_follows_material_design() {
    // Test spacing usage and semantic colors
    composeTestRule.onNode(hasTestTag("themed_content"))
        .assertUsesMaterialSpacing()
        .assertUsesSemanticColors()
}
```

## Performance Considerations

### Composition Optimization
- Use stable `key` parameters in `LazyColumn.items()` to prevent unnecessary recompositions
- Avoid unstable lambdas in modifiers; extract to stable variables when needed
- Use `rememberSaveable` for scroll state preservation across config changes

### Image Loading
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .build(),
    contentDescription = stringResource(R.string.image_description),
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f/9f)
)
```

## Migration Guide

### From Legacy Components
When updating existing components to follow these patterns:

1. **Extract hardcoded strings** to `strings.xml`
2. **Add content descriptions** to all images and icons
3. **Ensure touch targets** are at least 48dp
4. **Replace hardcoded colors** with semantic tokens
5. **Use Material spacing** instead of dp literals
6. **Add proper semantic properties** for accessibility
7. **Implement state preservation** with `rememberSaveable`
8. **Add comprehensive tests** for accessibility and Material compliance

### Common Issues and Fixes

| Issue | Symptom | Fix |
|-------|---------|-----|
| Hardcoded strings | `"Text"` instead of `stringResource()` | Extract to strings.xml |
| Small touch targets | `< 48dp` interactive elements | Add `minimumTouchTargetSize()` |
| Missing content descriptions | Images without descriptions | Add `contentDescription` parameter |
| Hardcoded colors | `Color(0xFF...)` | Use `MaterialTheme.colorScheme.*` |
| Inconsistent spacing | `16.dp`, `24.dp` literals | Use `MaterialTheme.spacing.*` |
| Poor contrast | Text hard to read | Test with color contrast tools |

---

This guide ensures all nanoAI UI components provide an excellent, accessible user experience while maintaining high performance and Material Design 3 compliance.
