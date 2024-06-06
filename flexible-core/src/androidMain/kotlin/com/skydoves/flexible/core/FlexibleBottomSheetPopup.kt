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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.lang.reflect.Field
import java.util.UUID

/**
 * Popup specific for flexible bottom sheet.
 */
@Composable
@InternalFlexibleApi
public actual fun FlexibleBottomSheetPopup(
  onDismissRequest: () -> Unit,
  windowInsets: WindowInsets,
  sheetState: FlexibleSheetState,
  content: @Composable BoxScope.() -> Unit,
) {
  val view = LocalView.current
  val id = rememberSaveable { UUID.randomUUID() }
  val parentComposition = rememberCompositionContext()
  val currentContent by rememberUpdatedState(content)
  val flexibleBottomSheetWindow = remember {
    FlexibleBottomSheetWindow(
      onDismissRequest = onDismissRequest,
      composeView = view,
      saveId = id,
      sheetState = sheetState,
    ).apply {
      setCustomContent(
        parent = parentComposition,
        content = {
          Box(
            Modifier
              .semantics { this.popup() }
              .windowInsetsPadding(windowInsets)
              .imePadding(),
          ) {
            currentContent()
          }
        },
      )
    }
  }

  if (!sheetState.skipHiddenState) {
    BackHandler { onDismissRequest() }
  }

  DisposableEffect(flexibleBottomSheetWindow) {
    flexibleBottomSheetWindow.show()
    onDispose {
      flexibleBottomSheetWindow.disposeComposition()
      flexibleBottomSheetWindow.dismiss()
    }
  }
}

/** Custom compose view for [FlexibleBottomSheet] */
@SuppressLint("ViewConstructor")
private class FlexibleBottomSheetWindow(
  private var onDismissRequest: () -> Unit,
  private val composeView: View,
  private val sheetState: FlexibleSheetState,
  saveId: UUID,
) :
  AbstractComposeView(composeView.context),
  ViewTreeObserver.OnGlobalLayoutListener,
  ViewRootForInspector {

  init {
    id = android.R.id.content
    // Set up view owners
    setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
    setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
    setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
    setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Popup:$saveId")
    // Enable children to draw their shadow by not clipping them
    clipChildren = false
  }

  private val windowManager =
    composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private var content: @Composable () -> Unit by mutableStateOf({})

  override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
    private set

  @Composable
  override fun Content() {
    content()
  }

  fun setCustomContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit,
  ) {
    parent?.let { setParentCompositionContext(it) }
    this.content = content
    shouldCreateCompositionOnAttachedToWindow = true
  }

  fun show() {
    windowManager.addView(this, getWindowParams())
  }

  fun dismiss() {
    setViewTreeLifecycleOwner(null)
    setViewTreeSavedStateRegistryOwner(null)
    composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
    windowManager.removeViewImmediate(this)
  }

  private fun getWindowParams(windowHeight: Int? = null): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
      // Position bottom sheet from the bottom of the screen
      gravity = Gravity.BOTTOM or Gravity.CENTER
      // Application panel window
      type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
      // Fill up the entire app view
      width = WindowManager.LayoutParams.MATCH_PARENT
      height = windowHeight ?: WindowManager.LayoutParams.WRAP_CONTENT
      // Format of screen pixels
      format = PixelFormat.TRANSLUCENT
      // Title used as fallback for a11y services
      title = "Pop-Up Window"
      // Get the Window token from the parent view
      token = composeView.applicationWindowToken
      // Remove default Window animations
      windowAnimations = 0x00000040

      val className = "android.view.WindowManager\$LayoutParams"
      val layoutParamsClass = Class.forName(className)

      val privateFlags: Field = layoutParamsClass.getField("privateFlags")
      val noAnim: Field = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")

      var privateFlagsValue: Int = privateFlags.getInt(this)
      val noAnimFlag: Int = noAnim.getInt(this)
      privateFlagsValue = privateFlagsValue or noAnimFlag
      privateFlags.setInt(this, privateFlagsValue)

      // Flags specific to flexible bottom sheet.
      flags = if (sheetState.isModal) {
        flags and (
          WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
          ).inv()
      } else {
        flags or WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
          WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
          WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
      }

      flags = if (sheetState.containSystemBars && !sheetState.isModal) {
        flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
      } else {
        flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
      }
    }
  }

  /**
   * Taken from PopupWindow. Calls [onDismissRequest] when back button is pressed.
   */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode == KeyEvent.KEYCODE_BACK) {
      if (keyDispatcherState == null) {
        return super.dispatchKeyEvent(event)
      }
      if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
        val state = keyDispatcherState
        state?.startTracking(event, this)
        return true
      } else if (event.action == KeyEvent.ACTION_UP) {
        val state = keyDispatcherState
        if (state != null && state.isTracking(event) && !event.isCanceled) {
          onDismissRequest()
          return true
        }
      }
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onGlobalLayout() {
    // No-op
  }
}
