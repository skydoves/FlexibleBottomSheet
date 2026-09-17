# Sheet Host

## Overview

`FlexibleBottomSheet` can be rendered in two places, chosen with the `sheetHost` parameter of
`rememberFlexibleBottomSheetState`.

| | `FlexibleSheetHost.Window` (default) | `FlexibleSheetHost.Inline` |
|---|---|---|
| Rendered in | a platform window of its own | the composition that declared it |
| Layering | always above every composable in the app | by composition order, like any other composable |
| Bounds | the whole screen on Android, the anchor's parent elsewhere | its parent layout |
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

!!! warning "The parent has to be an overlay with a bounded height"
    The sheet fills its container, so place it in a `Box`. In a `Column` or a `Row` it takes the
    remaining space from its siblings, and in a scrollable parent it has no height to size against
    and falls back to the screen height.

### Keeping the sheet above a bottom bar

Place the sheet inside the content slot of the `Scaffold`. The content padding keeps the sheet
above the bottom bar, and `Scaffold` places the body before the bars, so the bar also draws above
the sheet while it is being dragged.

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

Two behaviors follow from the sheet being a normal composable rather than a window, and are worth
knowing before switching:

- **Back ordering.** `OnBackPressedDispatcher` invokes callbacks last registered first. A window
  hosted sheet registers when its window is shown and therefore always wins. An inline sheet
  registers at its position in the composition, so a navigation host or drawer composed after it
  takes back first.
- **Dismiss keys outside Android.** A window hosted sheet dismisses on Escape on desktop, because
  its `Popup` sets `dismissOnBackPress`. An inline sheet has no window to carry that, so on the non
  Android targets it is dismissed by gesture or programmatically only.

!!! tip
    An inline sheet shares the host window, so a `TextField` inside it gets the normal
    copy/paste toolbar, tooltips and dropdown menus anchor correctly, and the software keyboard
    behaves exactly as it does elsewhere in your app.
