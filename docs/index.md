# Overview

<p align="center">
  <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview0.png?raw=true" width="270"/>
  <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview1.png?raw=true" width="270"/>
  <img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview2.png?raw=true" width="270"/>
</p>

FlexibleBottomSheet is an advanced Compose Multiplatform bottom sheet for segmented sizing, non-modal type, and allows interaction behind the bottom sheet similar to Google Maps. It also offers additional conveniences, including specifying sheet sizes, monitoring sheet states, and more customization.

## Features

- **Segmented Sizing**: Define custom sizes for fully expanded, intermediately expanded, and slightly expanded states.
- **Non-Modal Support**: Allow interaction with content behind the bottom sheet, similar to Google Maps.
- **Wrap Content**: Automatically size the sheet based on its content height.
- **Initial Value**: Start the sheet at a specific expanded state without animation.
- **State Monitoring**: Track and respond to sheet state changes dynamically.
- **Nested Scroll Support**: Seamless integration with scrollable components like `LazyColumn`.
- **Compose Multiplatform**: Supports Android, iOS, Desktop, Web (JS/WASM).

## Platforms

FlexibleBottomSheet supports Kotlin Multiplatform and the following platforms:

- Android
- iOS
- Desktop (JVM)
- Web (JavaScript)
- Web (WASM)

## Why FlexibleBottomSheet?

The standard `ModalBottomSheet` from Compose Material 3 is great for basic use cases, but it has limitations:

1. **Modal Only**: You can't interact with the content behind the sheet.
2. **Limited Sizing**: Only supports partially and fully expanded states.
3. **No Segmented States**: Can't define custom intermediate sizes.

FlexibleBottomSheet addresses all these limitations while maintaining API compatibility with the standard bottom sheet.

## Quick Start

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = 0.9f,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.15f,
    ),
    isModal = true,
    skipSlightlyExpanded = false,
  ),
  containerColor = Color.Black,
) {
  Text(
    modifier = Modifier
      .fillMaxWidth()
      .padding(8.dp),
    text = "This is Flexible Bottom Sheet",
    textAlign = TextAlign.Center,
    color = Color.White,
  )
}
```

<img src="https://github.com/skydoves/FlexibleBottomSheet/blob/main/previews/preview3.gif?raw=true" width="280px" />

## Find this repository useful?

Support it by joining [stargazers](https://github.com/skydoves/FlexibleBottomSheet/stargazers) for this repository.

Also, [follow me](https://github.com/skydoves) on GitHub for my next creations!
