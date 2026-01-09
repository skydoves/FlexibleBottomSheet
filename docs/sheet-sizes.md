# Sheet Sizes

## FlexibleSheetSize

`FlexibleSheetSize` allows you to customize the expanded size of the bottom sheet content based on its states. These constraints are calculated by multiplying the ratio with the maximum display height excluding the system bars (status and navigation bars).

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = 0.85f,
      intermediatelyExpanded = 0.45f,
      slightlyExpanded = 0.15f,
    ),
  )
) {
  // content
}
```

## Size Ratios

Each size value represents a ratio of the screen height:

| Property | Default | Description |
|----------|---------|-------------|
| `fullyExpanded` | 1.0f | 100% of screen height |
| `intermediatelyExpanded` | 0.5f | 50% of screen height |
| `slightlyExpanded` | 0.25f | 25% of screen height |

### Visual Example

| Fully (0.85) | Intermediately (0.45) | Slightly (0.15) |
| :----------: | :-------------------: | :-------------: |
| <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview5.png?raw=true" width="100%" /> | <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview6.png?raw=true" width="100%" /> | <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview7.png?raw=true" width="100%" /> |

## Wrap Content Size

Instead of using fixed ratios, you can make the bottom sheet size itself based on its content height by using `FlexibleSheetSize.WrapContent`. This is useful when you want the sheet to automatically fit its content.

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = FlexibleSheetSize.WrapContent,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.15f,
    ),
  )
) {
  // The sheet will wrap this content when fully expanded
  Column {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
  }
}
```

### How Wrap Content Works

- When the content is **smaller** than the screen, the sheet will size itself to fit the content.
- When the content is **larger** than the screen, it will be constrained to the screen height.

### Using Wrap Content for Multiple States

You can use `WrapContent` for any of the expanded states:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = FlexibleSheetSize.WrapContent,
      intermediatelyExpanded = FlexibleSheetSize.WrapContent,
      slightlyExpanded = 0.15f,
    ),
    skipSlightlyExpanded = false,
  )
) {
  // content
}
```

!!! tip "Best Practice"
    `WrapContent` works best with content that has a known, static height. For dynamic content like `LazyColumn`, consider using fixed ratios instead.

## Combining with Initial Value

You can combine `WrapContent` with `initialValue` to start the sheet at a specific wrap-content state:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    initialValue = FlexibleSheetValue.FullyExpanded,
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = FlexibleSheetSize.WrapContent,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.15f,
    ),
  )
) {
  // Sheet starts fully expanded, wrapping its content
  Column(modifier = Modifier.padding(16.dp)) {
    Text("Welcome!", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text("This sheet wraps its content.")
  }
}
```
