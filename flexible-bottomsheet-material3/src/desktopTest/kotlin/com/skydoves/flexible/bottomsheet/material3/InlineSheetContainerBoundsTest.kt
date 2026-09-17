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
package com.skydoves.flexible.bottomsheet.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression test for an inline sheet being measured against its own slot (issues #20 / #98).
 *
 * All of a sheet's sizes are a fraction of the height it may expand into. A window hosted sheet has
 * no container to measure against and falls back to the screen, which is why a sheet dropped into
 * `Scaffold` content was as tall as the whole screen and covered the bottom bar it was supposed to
 * stop above. An inline sheet is measured against the slot it was placed in, which the host has
 * already laid out and padded, so `fullyExpanded = 1.0f` means "all of this slot", not "all of the
 * screen".
 *
 * The window is split into a top bar, a content slot of exactly half the window, and a bottom bar,
 * and a fully expanded inline sheet is placed in the content slot. The painted sheet is then
 * measured from the frame rather than from the sheet's bounds, because the sheet's
 * `Modifier.offset` sits inside its semantics modifier and those bounds report the container
 * instead of the pixels.
 *
 * The same tree with [FlexibleSheetHost.Window] fails this test: the sheet is still a whole window
 * tall, so on this target it hangs over the top bar (the desktop popup is anchored to the bottom of
 * the slot and grows upward out of it) and on Android it runs down over the bottom bar instead.
 * Both are the same bug, and either way the painted sheet is not the size of the slot it was put
 * in.
 */
@OptIn(ExperimentalTestApi::class)
class InlineSheetContainerBoundsTest {

  @Test
  fun inlineSheet_isConfinedToTheSlotItWasPlacedIn() = runComposeUiTest {
    setContent { BarsAroundASlottedSheet(sheetHost = FlexibleSheetHost.Inline) }
    waitForIdle()

    val image = onNodeWithTag(RootTag).captureToImage()
    val slotTop = image.height / 4
    val slotBottom = image.height / 4 * 3
    val slotHeight = slotBottom - slotTop
    val tolerance = 2

    val paintedTop = image.firstSheetRow()
    val paintedBottom = image.lastSheetRow()
    assertTrue(paintedTop >= 0, "The sheet was not painted at all")

    assertTrue(
      paintedTop >= slotTop - tolerance,
      "The sheet spilled above its slot: painted from row $paintedTop, slot starts at $slotTop",
    )
    assertTrue(
      paintedBottom < slotBottom + tolerance,
      "The sheet covered the bottom bar: painted to row $paintedBottom, slot ends at $slotBottom",
    )

    val paintedHeight = paintedBottom - paintedTop + 1
    assertTrue(
      paintedHeight in (slotHeight - tolerance)..(slotHeight + tolerance),
      "A fully expanded inline sheet was $paintedHeight px tall, but its slot is $slotHeight px " +
        "of a ${image.height} px window",
    )

    // The bars around the slot are untouched, whatever the sheet did inside it.
    val pixels = image.toPixelMap()
    val topBar = pixels[pixels.width / 2, slotTop / 2]
    val bottomBar = pixels[pixels.width / 2, image.height - 2]
    assertTrue(topBar.isTopBarColor(), "The sheet painted over the top bar: $topBar")
    assertTrue(bottomBar.isBottomBarColor(), "The sheet painted over the bottom bar: $bottomBar")
  }

  @Test
  fun windowHostedSheet_spillsOutOfTheSlotItWasPlacedIn() = runComposeUiTest {
    setContent { BarsAroundASlottedSheet(sheetHost = FlexibleSheetHost.Window) }
    waitForIdle()

    val image = onNodeWithTag(RootTag).captureToImage()
    val slotTop = image.height / 4

    val paintedTop = image.firstSheetRow()
    assertTrue(paintedTop >= 0, "The sheet was not painted at all")

    // The counterpart of the test above, so the difference between the two hosts is pinned by an
    // assertion rather than only described. A window hosted sheet is a whole window tall and is
    // anchored to the bottom of the slot, so it grows up out of it and over the top bar.
    assertTrue(
      paintedTop < slotTop,
      "A window hosted sheet was confined to its slot: painted from row $paintedTop, slot starts " +
        "at $slotTop",
    )
  }

  @Composable
  private fun BarsAroundASlottedSheet(sheetHost: FlexibleSheetHost) {
    Column(Modifier.fillMaxSize().testTag(RootTag)) {
      Box(Modifier.fillMaxWidth().weight(1f).background(TopBarColor))

      // The "Scaffold content" slot: exactly half the window, a quarter of it from the top.
      Box(Modifier.fillMaxWidth().weight(2f).background(Color.White)) {
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = false,
          skipHiddenState = true,
          skipSlightlyExpanded = true,
          initialValue = FlexibleSheetValue.FullyExpanded,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 1f,
            intermediatelyExpanded = 0.5f,
          ),
          sheetHost = sheetHost,
        )
        FlexibleBottomSheet(
          onDismissRequest = {},
          sheetState = sheetState,
          containerColor = SheetColor,
          shape = RectangleShape,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(80.dp))
        }
      }

      Box(Modifier.fillMaxWidth().weight(1f).background(BottomBarColor))
    }
  }

  /** The topmost row painted with the sheet's container color, or `-1` when it is not on screen. */
  private fun ImageBitmap.firstSheetRow(): Int {
    val pixels = toPixelMap()
    val x = pixels.width / 2
    for (y in 0 until pixels.height) {
      if (pixels[x, y].isSheetColor()) return y
    }
    return -1
  }

  /** The bottommost row painted with the sheet's container color, or `-1`. */
  private fun ImageBitmap.lastSheetRow(): Int {
    val pixels = toPixelMap()
    val x = pixels.width / 2
    for (y in pixels.height - 1 downTo 0) {
      if (pixels[x, y].isSheetColor()) return y
    }
    return -1
  }
}

private const val RootTag = "root"

/** Three opaque colors that cannot be confused with each other or with the white slot. */
private val SheetColor = Color.Blue
private val TopBarColor = Color.Yellow
private val BottomBarColor = Color.Green

private fun Color.isSheetColor(): Boolean = blue > 0.5f && red < 0.5f && green < 0.5f

private fun Color.isTopBarColor(): Boolean = red > 0.5f && green > 0.5f && blue < 0.5f

private fun Color.isBottomBarColor(): Boolean = green > 0.5f && red < 0.5f && blue < 0.5f
