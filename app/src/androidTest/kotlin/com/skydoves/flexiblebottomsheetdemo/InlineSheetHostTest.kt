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
package com.skydoves.flexiblebottomsheetdemo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device level test for [FlexibleSheetHost.Inline], the layering claim behind issues #75, #71, #37,
 * #20 and #98.
 *
 * A window hosted sheet is a separate platform window, therefore above every composable in the
 * app whatever the composition order. An inline sheet is part of the composition, so a sibling
 * composed after it draws, and receives touches, on top of it.
 *
 * Taps go through [UiDevice] so they travel the real window dispatch. Compose's own input injection
 * targets a chosen compose root and could not tell the two hosts apart.
 */
@RunWith(AndroidJUnit4::class)
class InlineSheetHostTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Test
  fun aFooterComposedAfterAnInlineSheetReceivesTheTap() {
    val taps = tapFooterOverSheet(FlexibleSheetHost.Inline)

    assertEquals("The footer did not receive the tap", 1, taps.footer)
    assertEquals("The sheet took a tap meant for the footer", 0, taps.sheet)
  }

  @Test
  fun aWindowHostedSheetTakesTheTapFromAFooterComposedAfterIt() {
    val taps = tapFooterOverSheet(FlexibleSheetHost.Window)

    // Both sides on purpose: a bare "the footer got nothing" would also pass if the tap had missed
    // or the layout had never composed.
    assertEquals("The footer received a tap it should not have", 0, taps.footer)
    assertEquals("The sheet window did not take the tap", 1, taps.sheet)
  }

  /**
   * Lays a footer over a sheet expanded far enough to sit underneath it, taps the footer through
   * the
   * real input pipeline, and reports where the tap landed.
   */
  private fun tapFooterOverSheet(sheetHost: FlexibleSheetHost): Taps {
    var footerTaps = 0
    var sheetTaps = 0
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        sheetHost = sheetHost,
        isModal = false,
        skipHiddenState = true,
        skipSlightlyExpanded = false,
        initialValue = FlexibleSheetValue.IntermediatelyExpanded,
        flexibleSheetSize = FlexibleSheetSize(
          fullyExpanded = 1.0f,
          intermediatelyExpanded = 0.5f,
          slightlyExpanded = 0.2f,
        ),
      )

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = { },
          sheetState = sheetState,
          dragHandle = null,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clickable { sheetTaps++ },
          )
        }

        Footer(onClick = { footerTaps++ })
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.IntermediatelyExpanded
    }
    composeTestRule.waitForIdle()

    // `boundsInWindow` is relative to the activity window, while UiDevice injects at display
    // coordinates. They only coincide when the window starts at the display origin.
    val bounds = composeTestRule.onNodeWithTag(FOOTER_TAG).fetchSemanticsNode().boundsInWindow
    val windowOrigin = IntArray(2)
    composeTestRule.activity.window.decorView.getLocationOnScreen(windowOrigin)
    val clicked = device.click(
      windowOrigin[0] + bounds.center.x.toInt(),
      windowOrigin[1] + bounds.center.y.toInt(),
    )
    assertTrue("The tap was not injected", clicked)
    device.waitForIdle()
    composeTestRule.waitForIdle()

    return Taps(footer = footerTaps, sheet = sheetTaps)
  }

  private data class Taps(val footer: Int, val sheet: Int)

  private companion object {
    const val FOOTER_TAG = "footer"
    const val TIMEOUT_MILLIS = 5_000L
  }
}

/**
 * A bar pinned to the bottom of its parent, composed after the sheet.
 */
@Composable
private fun BoxScope.Footer(onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .align(Alignment.BottomCenter)
      .fillMaxWidth()
      .height(72.dp)
      .testTag("footer")
      .clickable { onClick() },
  )
}
