# Sheet Host

## Overview

`FlexibleBottomSheet` can be rendered in two places, chosen with the `sheetHost` parameter of
`rememberFlexibleBottomSheetState`.

| | `FlexibleSheetHost.Window` (default) | `FlexibleSheetHost.Inline` |
|---|---|---|
| Rendered in | a platform window of its own | the composition that declared it |
| Layering | always above every composable in the app | by composition order, like any other composable |
| Bounds | the whole screen | its parent layout |
| Can be covered by a drawer, app bar or footer | no | yes |
| Non-modal touch pass-through | window level, whole-window granularity | per composable, exact |
| Text selection toolbar, tooltips, dropdowns | own window context | shares the host window |

`Window` is the default, so existing code is unchanged.

## Inline

```kotlin
val sheetState = rememberFlexibleBottomSheetState(
  sheetHost = FlexibleSheetHost.Inline,
  isModal = false,
  skipSlightlyExpanded = false,
)

Box(modifier = Modifier.fillMaxSize()) {
  MapContent()

  FlexibleBottomSheet(
    onDismissRequest = { },
    sheetState = sheetState,
  ) {
    LocationDetails()
  }
}
```

An inline sheet is bottom aligned inside its parent and expands into the space that parent offers,
so where you place it is what it covers.

### Keeping the sheet above a bottom bar

Place the sheet inside the content slot of the `Scaffold`. The content padding keeps the sheet
above the bottom bar, and the bottom bar is composed after the content, so it also draws above the
sheet while it is being dragged.

```kotlin
Scaffold(
  bottomBar = { NavigationBar { /* ... */ } },
) { contentPadding ->
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(contentPadding),
  ) {
    ScreenContent()

    FlexibleBottomSheet(
      onDismissRequest = { },
      sheetState = rememberFlexibleBottomSheetState(
        sheetHost = FlexibleSheetHost.Inline,
        isModal = false,
        skipSlightlyExpanded = false,
      ),
    ) {
      SheetContent()
    }
  }
}
```

### Letting a navigation drawer cover the sheet

Compose the drawer after the sheet and it draws above it.

```kotlin
ModalNavigationDrawer(
  drawerContent = { DrawerSheet() },
) {
  Box(modifier = Modifier.fillMaxSize()) {
    ScreenContent()

    FlexibleBottomSheet(
      onDismissRequest = { },
      sheetState = rememberFlexibleBottomSheetState(
        sheetHost = FlexibleSheetHost.Inline,
      ),
    ) {
      SheetContent()
    }
  }
}
```

### A sticky footer above the sheet

Anything composed after the sheet stays above it and does not move with it.

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
  FlexibleBottomSheet(
    onDismissRequest = { },
    sheetState = rememberFlexibleBottomSheetState(sheetHost = FlexibleSheetHost.Inline),
  ) {
    SheetContent()
  }

  Button(
    onClick = { },
    modifier = Modifier
      .align(Alignment.BottomCenter)
      .padding(16.dp),
  ) {
    Text(text = "Continue")
  }
}
```

## When to keep `Window`

Keep the default when the sheet must cover the entire screen regardless of where it was declared,
for example a sheet opened from a nested screen that should still cover the app's navigation bar,
or a sheet that must survive its parent layout being clipped.

!!! tip
    An inline sheet shares the host window, so a `TextField` inside it gets the normal
    copy/paste toolbar, tooltips and dropdown menus anchor correctly, and the software keyboard
    behaves exactly as it does elsewhere in your app.
