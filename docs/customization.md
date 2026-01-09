# Customization

## Nested Scroll

`FlexibleBottomSheet` inherently supports nested scrolling, allowing seamless integration with components like `LazyColumn`, `LazyRow`, and others.

### Enabling/Disabling Nested Scroll

If you wish to disable nested scrolling, set `allowNestedScroll` to `false`:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    allowNestedScroll = false
  ),
) {
  LazyColumn {
    items(100) { index ->
      Text("Item $index")
    }
  }
}
```

### Comparison

| Enabled | Disabled |
| :-----: | :------: |
| <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview11.gif?raw=true" width="100%" /> | <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview12.gif?raw=true" width="100%" /> |

## Dynamic Content

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

## Styling

### Container Color

Customize the background color of the sheet:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  containerColor = Color.Black,
) {
  // content with white text for contrast
}
```

### Shape

Customize the shape of the sheet:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
) {
  // content
}
```

### Scrim Color (Modal Only)

For modal sheets, customize the scrim (overlay) color:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  scrimColor = Color.Black.copy(alpha = 0.5f),
  sheetState = rememberFlexibleBottomSheetState(
    isModal = true
  ),
) {
  // content
}
```

### Drag Handle

Customize or hide the drag handle:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  dragHandle = {
    // Custom drag handle
    Box(
      modifier = Modifier
        .padding(vertical = 8.dp)
        .width(40.dp)
        .height(4.dp)
        .background(Color.Gray, RoundedCornerShape(2.dp))
    )
  },
) {
  // content
}
```

To hide the drag handle:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  dragHandle = null,
) {
  // content
}
```

## Animation with Orbital

You can implement dynamic content animations by combining FlexibleBottomSheet with [Orbital](https://github.com/skydoves/Orbital):

<img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview9.gif?raw=true" width="320px">

## Window Insets

Handle system bars and window insets:

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  windowInsets = WindowInsets.systemBars,
) {
  // content respects system bars
}
```
