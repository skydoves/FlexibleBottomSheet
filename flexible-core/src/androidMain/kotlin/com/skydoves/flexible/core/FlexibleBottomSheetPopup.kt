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
          // This BackHandler is registered on the *host activity's* dispatcher, so it must be
          // disabled while the sheet rests hidden. Otherwise a dismissed sheet keeps intercepting
          // the activity's back press and back silently does nothing.
          if (!sheetState.skipHiddenState) {
            BackHandler(enabled = sheetState.currentValue != FlexibleSheetValue.Hidden) {
              onDismissRequest()
            }
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

  // A fully hidden sheet is still composed, and its window still covers the screen and still holds
  // input focus, so without this a dismissed sheet keeps swallowing the touches, key events and IME
  // requests meant for the content behind it (#15). The window stays interactive for the whole hide
  // animation and only steps aside once the sheet has come to rest in the hidden state.
  //
  // This runs in a SideEffect rather than a LaunchedEffect on purpose: a LaunchedEffect body is
  // dispatched and would apply the flags a frame late, leaving the first frame of a re-opening sheet
  // untouchable. SideEffect runs synchronously while changes are applied, after `show()`.
  val isSheetInteractive = sheetState.targetValue != FlexibleSheetValue.Hidden ||
    sheetState.currentValue != FlexibleSheetValue.Hidden

  SideEffect {
    flexibleBottomSheetWindow.updateParentComposition(parentComposition)
    flexibleBottomSheetWindow.updateDismissRequest(onDismissRequest)
    flexibleBottomSheetWindow.setSheetInteractive(isSheetInteractive)
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
   * Touches that land inside a window are consumed by that window even when no view handles them,
   * and a focused window receives the key events and IME requests of the whole app, so hiding the
   * sheet visually is not enough to hand input back to the content behind it.
   */
  fun setSheetInteractive(interactive: Boolean) {
    if (isSheetInteractive == interactive) return
    isSheetInteractive = interactive

    // Before [show] the flag is simply picked up by the params [show] builds, so there is nothing to
    // update yet. `isAttachedToWindow` must not be used as the guard here: the view is only attached
    // on the first traversal after `addView`, which can land after this runs, and skipping the
    // update then would leave the window flags permanently out of sync with the sheet.
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

    // On API 33+ the platform routes back through OnBackInvokedDispatcher and never delivers
    // KEYCODE_BACK to the view tree, but only for apps that opted into predictive back. For apps
    // that did not, this registration is rejected (logged, not thrown) and the platform re-injects
    // KEYCODE_BACK into this window instead, where [dispatchKeyEvent] picks it up. Exactly one of
    // the two paths is ever live, so they cannot both fire for a single press.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val callback = OnBackInvokedCallback { onDismissRequest() }
      val dispatcher = findOnBackInvokedDispatcher()
      dispatcher?.registerOnBackInvokedCallback(
        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        callback,
      )
      onBackInvokedCallback = callback
      onBackInvokedDispatcher = dispatcher
    }
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

      // A window added through WindowManager starts at SOFT_INPUT_ADJUST_UNSPECIFIED, and a theme's
      // windowSoftInputMode only ever reaches the activity's own window. Without ADJUST_RESIZE the
      // platform leaves the IME out of this window's compat and visible insets, which is what made
      // Modifier.imePadding() measure zero and let the keyboard cover the sheet content (#16).
      // STATE_UNCHANGED keeps the sheet from toggling the keyboard as it is added or relaid out.
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
   * view tree.
   *
   * This used to return `true` for every back event, including a **canceled** `ACTION_UP`, while
   * only acting on a non canceled one. Window focus moves between this panel and the host activity
   * on any touch outside a non modal sheet, and every such transfer cancels the in flight key
   * event, so that branch silently ate the press and stopped it from reaching the activity: on the
   * first screen of the back stack, where nothing else would have handled it either, back simply
   * did nothing (#92). A press that is not acted on is now forwarded instead of swallowed.
   */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.keyCode != KeyEvent.KEYCODE_BACK) {
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
     * `WindowManager.LayoutParams.privateFlags` and `PRIVATE_FLAG_NO_MOVE_ANIMATION` are hidden
     * platform API used to suppress the window move animation.
     *
     * They are resolved once for the process instead of on every window layout pass: the window
     * params are now rebuilt whenever the sheet's interactivity changes, and a build that
     * blocklists these members must degrade to "the window animates" rather than throw on every
     * update.
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
