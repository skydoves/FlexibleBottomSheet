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
  handlesBackGesture: Boolean,
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
          // Registered on the host activity's dispatcher, for presses that arrive while the
          // activity holds focus rather than this panel. Disabled whenever the sheet would not act,
          // otherwise a hidden sheet keeps intercepting the activity's back press.
          BackHandler(
            enabled = handlesBackGesture &&
              sheetState.currentValue != FlexibleSheetValue.Hidden,
          ) {
            onDismissRequest()
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

  // A hidden sheet is still composed and its window still owns the screen and the input focus, so
  // without this it keeps swallowing touches, key events and IME requests meant for the content
  // behind it (#15). SideEffect rather than LaunchedEffect: the latter is dispatched and would
  // apply the flags a frame late, leaving a re-opening sheet untouchable on its first frame.
  val isSheetInteractive = sheetState.targetValue != FlexibleSheetValue.Hidden ||
    sheetState.currentValue != FlexibleSheetValue.Hidden

  SideEffect {
    flexibleBottomSheetWindow.updateParentComposition(parentComposition)
    flexibleBottomSheetWindow.updateDismissRequest(onDismissRequest)
    flexibleBottomSheetWindow.setSheetInteractive(isSheetInteractive)
    flexibleBottomSheetWindow.setBackGestureHandled(handlesBackGesture)
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
  }

  private val windowManager =
    composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  private var content: @Composable () -> Unit by mutableStateOf({})
  private var onBackInvokedCallback: OnBackInvokedCallback? = null
  private var onBackInvokedDispatcher: OnBackInvokedDispatcher? = null
  private var isSheetInteractive: Boolean = true
  private var isAddedToWindowManager: Boolean = false
  private var handlesBackGesture: Boolean = true

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

  /**
   * Adopts the latest dismiss lambda.
   *
   * The window itself is created inside a keyless `remember`, so without this the back key would
   * keep invoking the lambda captured during the very first composition.
   */
  fun updateDismissRequest(onDismissRequest: () -> Unit) {
    this.onDismissRequest = onDismissRequest
  }

  /**
   * Toggles whether this window takes part in input at all.
   *
   * A window consumes every touch inside its bounds even when no view handles it, and a focused
   * window receives the app's key events and IME requests, so hiding the sheet visually is not
   * enough to hand input back to the content behind it.
   */
  fun setSheetInteractive(interactive: Boolean) {
    if (isSheetInteractive == interactive) return
    isSheetInteractive = interactive

    // Before [show] the params it builds pick the flag up anyway. Not `isAttachedToWindow`: the
    // view only attaches on the first traversal after `addView`, which can land after this runs,
    // and skipping the update then desyncs the flags permanently.
    if (!isAddedToWindowManager) return

    windowManager.updateViewLayout(this, getWindowParams())
    if (interactive) {
      requestFocus()
    }
  }

  fun show() {
    windowManager.addView(this, getWindowParams())
    isAddedToWindowManager = true
    requestFocus()

    syncOnBackInvokedCallback()
  }

  /**
   * Registers or unregisters the predictive back callback to match [handlesBackGesture].
   *
   * An [OnBackInvokedCallback] consumes the gesture unconditionally and cannot decline it, so
   * "should the sheet handle back" is expressed by whether it is registered at all, mirroring
   * `BackHandler(enabled = ...)`.
   *
   * On API 33+ predictive back never delivers KEYCODE_BACK to the view tree, but only for apps that
   * opted in. For apps that did not, this registration is rejected and the platform re-injects the
   * key here instead, where [dispatchKeyEvent] takes it. Only ever one of the two is live.
   */
  private fun syncOnBackInvokedCallback() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    if (handlesBackGesture && onBackInvokedCallback == null) {
      val callback = OnBackInvokedCallback { onDismissRequest() }
      val dispatcher = findOnBackInvokedDispatcher()
      dispatcher?.registerOnBackInvokedCallback(
        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        callback,
      )
      onBackInvokedCallback = callback
      onBackInvokedDispatcher = dispatcher
    } else if (!handlesBackGesture) {
      onBackInvokedCallback?.let { callback ->
        onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(callback)
      }
      onBackInvokedCallback = null
      onBackInvokedDispatcher = null
    }
  }

  /**
   * Declares whether a back gesture would currently change the sheet at all.
   */
  fun setBackGestureHandled(handled: Boolean) {
    if (handlesBackGesture == handled) return
    handlesBackGesture = handled
    if (!isAddedToWindowManager) return

    syncOnBackInvokedCallback()
  }

  fun dismiss() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      onBackInvokedCallback?.let { callback ->
        onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(callback)
      }
      onBackInvokedCallback = null
      onBackInvokedDispatcher = null
    }

    setViewTreeLifecycleOwner(null)
    setViewTreeSavedStateRegistryOwner(null)
    composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
    windowManager.removeViewImmediate(this)
    isAddedToWindowManager = false
  }

  private fun getWindowParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
      // Application panel window
      type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
      // Fill up the entire app view
      width = WindowManager.LayoutParams.MATCH_PARENT

      // For modal sheets with edge-to-edge, use MATCH_PARENT to cover system bars.
      // For non-modal sheets, use WRAP_CONTENT to allow touch-through (Google Maps style).
      if (isEdgeToEdge && sheetState.isModal) {
        height = WindowManager.LayoutParams.MATCH_PARENT
        gravity = Gravity.TOP or Gravity.CENTER
      } else {
        height = WindowManager.LayoutParams.WRAP_CONTENT
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

      // A WindowManager-added window starts at SOFT_INPUT_ADJUST_UNSPECIFIED, and a theme's
      // windowSoftInputMode only reaches the activity's own window. Without ADJUST_RESIZE the IME
      // is left out of this window's insets, which is why imePadding() measured zero (#16).
      // STATE_UNCHANGED stops the sheet toggling the keyboard as it is added or relaid out.
      softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
        WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED

      noMoveAnimation?.let { (privateFlags, noMoveAnimationFlag) ->
        privateFlags.setInt(this, privateFlags.getInt(this) or noMoveAnimationFlag)
      }

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

      if (!isSheetInteractive) {
        flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
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
   * Dismisses the sheet on the back key, for the platforms and apps that still deliver it to the
   * view tree (API 32 and below, and API 33+ for apps that did not opt into predictive back).
   *
   * The sheet claims the key only when it would act on it. A press it cannot act on, and a canceled
   * press, go to `super` rather than being reported as consumed. No other window would pick those
   * up, but key input inside the sheet content can still observe them.
   */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode != KeyEvent.KEYCODE_BACK || !handlesBackGesture) {
      return super.dispatchKeyEvent(event)
    }
    if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
      onDismissRequest()
      return true
    }
    // Consume the matching DOWN so nothing else starts a back gesture for the same press.
    if (event.action == KeyEvent.ACTION_DOWN && !event.isCanceled) {
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  override fun onGlobalLayout() {
    // No-op
  }

  private companion object {
    /**
     * Hidden platform API used to suppress the window move animation, resolved once per process
     * rather than on every window layout pass. A build that blocklists these members degrades to
     * "the window animates" instead of throwing on every update.
     */
    val noMoveAnimation: Pair<Field, Int>? = runCatching {
      val layoutParamsClass = Class.forName("android.view.WindowManager\$LayoutParams")
      val privateFlags: Field = layoutParamsClass.getField("privateFlags")
      val noMoveAnimationFlag: Field =
        layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")
      privateFlags to noMoveAnimationFlag.getInt(null)
    }.getOrNull()
  }
}
