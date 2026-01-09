# Sheet State

## FlexibleBottomSheetState

`FlexibleBottomSheetState` is a crucial concept that must be bound to `FlexibleBottomSheet` to manage its state changes. It also enables you to customize UI/UX behaviors for the bottom sheet and take manual control over expanding/hiding the bottom sheet.

You can remember the `FlexibleBottomSheetState` by using `rememberFlexibleBottomSheetState`:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    skipSlightlyExpanded = false,
    skipIntermediatelyExpanded = false,
    isModal = true,
    allowNestedScroll = true,
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = 1.0f,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.25f
    )
  ),
) {
  // content
}
```

## Expanded States

The flexible bottom sheet offers four primary sheet states known as `FlexibleSheetValue`:

| State | Description |
|-------|-------------|
| **Fully Expanded** | The sheet is visible at its fully-expanded height. This is mandatory and cannot be skipped. |
| **Intermediately Expanded** | The sheet is visible at an intermediate expanded height. Can be skipped by setting `skipIntermediatelyExpanded` to `true`. |
| **Slightly Expanded** | The sheet is visible at a slightly expanded height. Skipped by default, enable by setting `skipSlightlyExpanded` to `false`. |
| **Hidden** | The sheet is completely not visible on the screen. To keep the sheet always visible, set `skipHiddenState` to `true`. |

### Skipping States

You can skip the **Intermediately Expanded** and **Slightly Expanded** states:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    skipSlightlyExpanded = false,        // Enable slightly expanded
    skipIntermediatelyExpanded = false   // Enable intermediately expanded
  ),
) {
  // content
}
```

## Initial Value

You can specify the initial expanded state when the bottom sheet first appears by using the `initialValue` parameter. This allows you to start the sheet at a specific state without animation:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    initialValue = FlexibleSheetValue.FullyExpanded,
    skipSlightlyExpanded = false,
  ),
) {
  // content
}
```

Available initial values:

- `FlexibleSheetValue.Hidden` (default)
- `FlexibleSheetValue.SlightlyExpanded`
- `FlexibleSheetValue.IntermediatelyExpanded`
- `FlexibleSheetValue.FullyExpanded`

!!! warning "Compatibility with Skip Settings"
    The `initialValue` must be compatible with the skip settings. For example:

    - If `skipSlightlyExpanded = true`, you cannot set `initialValue = FlexibleSheetValue.SlightlyExpanded`
    - If `skipIntermediatelyExpanded = true`, you cannot set `initialValue = FlexibleSheetValue.IntermediatelyExpanded`

## Manual Control

You can expand or hide the bottom sheet manually by utilizing the `FlexibleBottomSheetState`:

<img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview4.gif?raw=true" width="300px" align="right">

```kotlin
val scope = rememberCoroutineScope()
val sheetState = rememberFlexibleBottomSheetState(
  flexibleSheetSize = FlexibleSheetSize(fullyExpanded = 0.9f),
  isModal = true,
  skipSlightlyExpanded = false,
)

FlexibleBottomSheet(
  sheetState = sheetState,
  containerColor = Color.Black,
  onDismissRequest = onDismissRequest
) {
  Button(
    modifier = Modifier.align(Alignment.CenterHorizontally),
    onClick = {
      scope.launch {
        when (sheetState.swipeableState.currentValue) {
          FlexibleSheetValue.SlightlyExpanded ->
            sheetState.intermediatelyExpand()
          FlexibleSheetValue.IntermediatelyExpanded ->
            sheetState.fullyExpand()
          else -> sheetState.hide()
        }
      }
    },
  ) {
    Text(text = "Expand Or Hide")
  }
}
```

### Available Methods

| Method | Description |
|--------|-------------|
| `show()` | Shows the bottom sheet |
| `hide()` | Hides the bottom sheet |
| `fullyExpand()` | Expands to fully expanded state |
| `intermediatelyExpand()` | Expands to intermediately expanded state |
| `slightlyExpand()` | Expands to slightly expanded state |

## Monitoring State Changes

You can dynamically compose your bottom sheet content by tracking the bottom sheet state changes using the `onTargetChanges` callback:

<img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview8.gif?raw=true" width="290px" align="right">

```kotlin
var currentSheetTarget by remember {
  mutableStateOf(FlexibleSheetValue.IntermediatelyExpanded)
}

FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    skipSlightlyExpanded = false
  ),
  onTargetChanges = { sheetValue ->
    currentSheetTarget = sheetValue
  },
  containerColor = Color.Black,
) {
  Text(
    modifier = Modifier
      .fillMaxWidth()
      .padding(8.dp),
    text = "This is Flexible Bottom Sheet",
    textAlign = TextAlign.Center,
    color = Color.White,
    fontSize = when (currentSheetTarget) {
      FlexibleSheetValue.FullyExpanded -> 28.sp
      FlexibleSheetValue.IntermediatelyExpanded -> 20.sp
      else -> 12.sp
    },
  )
}
```
