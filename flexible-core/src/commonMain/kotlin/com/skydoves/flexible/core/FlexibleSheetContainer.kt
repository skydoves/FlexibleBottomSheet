/*
 * Designed and developed by 2023 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skydoves.flexible.core

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Hosts the bottom sheet content according to [FlexibleSheetState.sheetHost].
 */
@Composable
@InternalFlexibleApi
public fun FlexibleSheetContainer(
  onDismissRequest: () -> Unit,
  windowInsets: WindowInsets,
  sheetState: FlexibleSheetState,
  handlesBackGesture: Boolean,
  content: @Composable BoxScope.() -> Unit,
) {
  when (sheetState.sheetHost) {
    FlexibleSheetHost.Window -> FlexibleBottomSheetPopup(
      onDismissRequest = onDismissRequest,
      windowInsets = windowInsets,
      sheetState = sheetState,
      handlesBackGesture = handlesBackGesture,
      content = content,
    )

    FlexibleSheetHost.Inline -> InlineFlexibleSheetContainer(
      onDismissRequest = onDismissRequest,
      windowInsets = windowInsets,
      sheetState = sheetState,
      handlesBackGesture = handlesBackGesture,
      content = content,
    )
  }
}

/**
 * Hosts the sheet inside the current composition instead of a platform window.
 *
 * The container itself installs no pointer input, and a layout node without pointer input is not a
 * hit test target in Compose, so touches that miss the sheet reach whatever is behind it. That is
 * what gives a non modal inline sheet real touch pass through without any window level flag, and it
 * is also why the modal scrim, which does install pointer input, is the only thing that blocks the
 * content behind a modal sheet.
 */
@Composable
private fun InlineFlexibleSheetContainer(
  onDismissRequest: () -> Unit,
  windowInsets: WindowInsets,
  sheetState: FlexibleSheetState,
  handlesBackGesture: Boolean,
  content: @Composable BoxScope.() -> Unit,
) {
  FlexibleSheetBackHandler(
    enabled = handlesBackGesture && sheetState.isVisible,
  ) {
    onDismissRequest()
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .then(
        if (sheetState.containSystemBars) {
          Modifier
        } else {
          Modifier.windowInsetsPadding(windowInsets)
        },
      )
      .imePadding(),
  ) {
    // An inline sheet is measured against the slot it was placed in, not against the screen, so a
    // sheet inside Scaffold content stops above the bottom bar instead of covering it.
    CompositionLocalProvider(LocalFlexibleSheetMaxHeight provides maxHeight) {
      content()
    }
  }
}

/**
 * The height the sheet may expand into, or [Dp.Unspecified] when the sheet is hosted in a window of
 * its own and may use the whole screen.
 */
internal val LocalFlexibleSheetMaxHeight = compositionLocalOf { Dp.Unspecified }

/**
 * The height a sheet may expand into, which is what all of its sizes are a fraction of.
 *
 * An inline sheet is measured against the slot it was placed in, and that slot was already padded
 * for the window insets and the keyboard, so its height is the room actually available: a sheet
 * inside `Scaffold` content stops above the bottom bar instead of covering it.
 *
 * A window hosted sheet has no such container. A modal one falls back to the screen height minus the
 * keyboard, because it fills an ime padded container whose height shrinks while `screenHeight()`
 * does not (#16). A non-modal one is its own explicit height, so the two never disagreed and the
 * screen height is used as is.
 */
@Composable
@InternalFlexibleApi
public fun sheetMaxHeight(sheetState: FlexibleSheetState): Dp {
  val containerMaxHeight = LocalFlexibleSheetMaxHeight.current
  if (containerMaxHeight.isSpecified) {
    return containerMaxHeight
  }
  if (!sheetState.isModal) {
    return screenHeight()
  }

  val density = LocalDensity.current
  val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
  return (screenHeight() - imeHeight).coerceAtLeast(1.dp)
}
