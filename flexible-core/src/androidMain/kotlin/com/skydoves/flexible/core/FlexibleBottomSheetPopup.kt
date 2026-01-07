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
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
  val isEdgeToEdge = isEdgeToEdgeEnabled(view)
  val onBackPressedDispatcherOwner = view.findViewTreeOnBackPressedDispatcherOwner()

  val flexibleBottomSheetWindow = remember {
    FlexibleBottomSheetWindow(
      onDismissRequest = onDismissRequest,
      composeView = view,
      sheetState = sheetState,
      isEdgeToEdge = isEdgeToEdge,
      onBackPressedDispatcherOwner = onBackPressedDispatcherOwner,
      saveId = id,
    ).apply {
      setCustomContent(
        parent = parentComposition,
        content = {
          if (!sheetState.skipHiddenState) {
            BackHandler { onDismissRequest() }
          }
          Box(
            Modifier
              .semantics { this.popup() }
              .then(
                if (sheetState.containSystemBars || isEdgeToEdge) {
                  Modifier
                } else {
                  Modifier.windowInsetsPadding(windowInsets)
                },
              )
              .imePadding(),
          ) {
            currentContent()
          }
        },
      )
    }
  }

  SideEffect {
    flexibleBottomSheetWindow.updateParentComposition(parentComposition)
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
  private val isEdgeToEdge: Boolean,
  onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner?,
  saveId: UUID,
) :
  AbstractComposeView(composeView.context),
  ViewTreeObserver.OnGlobalLayoutListener,
  ViewRootForInspector {

  init {
    id = android.R.id.content
    setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
    setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
    setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
    onBackPressedDispatcherOwner?.let { setViewTreeOnBackPressedDispatcherOwner(it) }
    setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Popup:$saveId")
    clipChildren = false
    isFocusable = true
    isFocusableInTouchMode = true

    setOnKeyListener { _, keyCode, event ->
      if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
        onDismissRequest()
        true
      } else {
        false
      }
    }
  }

  private val windowManager =
    composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private var content: @Composable () -> Unit by mutableStateOf({})
  private var onBackInvokedCallback: OnBackInvokedCallback? = null

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

  fun updateParentComposition(parent: CompositionContext) {
    setParentCompositionContext(parent)
  }

  fun show() {
    windowManager.addView(this, getWindowParams())
    requestFocus()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      onBackInvokedCallback = OnBackInvokedCallback { onDismissRequest() }
      findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        onBackInvokedCallback!!,
      )
    }
  }

  fun dismiss() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      onBackInvokedCallback?.let { callback ->
        findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(callback)
      }
      onBackInvokedCallback = null
    }

    setViewTreeLifecycleOwner(null)
    setViewTreeSavedStateRegistryOwner(null)
    composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
    windowManager.removeViewImmediate(this)
  }

  private fun getWindowParams(windowHeight: Int? = null): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
      // Application panel window
      type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
      // Fill up the entire app view
      width = WindowManager.LayoutParams.MATCH_PARENT

      // When edge-to-edge is enabled, use MATCH_PARENT height and TOP gravity
      // to properly fill the screen including system bars.
      // Otherwise, use WRAP_CONTENT with BOTTOM gravity for traditional behavior.
      if (isEdgeToEdge) {
        height = WindowManager.LayoutParams.MATCH_PARENT
        gravity = Gravity.TOP or Gravity.CENTER
      } else {
        height = windowHeight ?: WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.BOTTOM or Gravity.CENTER
      }
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

      flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()

      flags = if (sheetState.isModal) {
        flags and (
          WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
          ).inv()
      } else {
        // For non-modal: allow window to be focusable for input fields,
        // but touches outside the window bounds pass through to windows behind it
        flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
          WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
          WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
      }

      // Use FLAG_LAYOUT_NO_LIMITS to extend into system bars when:
      // 1. Edge-to-edge mode is detected (enableEdgeToEdge() or Android 15+), OR
      // 2. containSystemBars is explicitly set to true (for non-modal sheets)
      // This allows the scrim and content to fully cover the status bar area.
      flags = if (isEdgeToEdge || (sheetState.containSystemBars && !sheetState.isModal)) {
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
      if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
        onDismissRequest()
        return true
      }
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onGlobalLayout() {
    // No-op
  }
}
