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

import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

/**
 * Detects if edge-to-edge mode is enabled for the current window.
 *
 * Edge-to-edge is considered enabled when:
 * - On Android 15 (API 35) or higher with targetSdk 35+: Always enabled (enforced by system)
 * - On earlier versions: When `WindowCompat.setDecorFitsSystemWindows(window, false)` was called
 *
 * @return true if edge-to-edge mode is enabled, false otherwise
 */
@Composable
internal fun isEdgeToEdgeEnabled(): Boolean {
  val view = LocalView.current
  return isEdgeToEdgeEnabled(view)
}

/**
 * Detects if edge-to-edge mode is enabled for the given view's window.
 */
internal fun isEdgeToEdgeEnabled(view: View): Boolean {
  // On Android 15 (API 35) and higher, edge-to-edge is enforced for apps targeting SDK 35+
  if (Build.VERSION.SDK_INT >= 35) {
    return true
  }

  // On earlier versions, check if decorFitsSystemWindows is set to false
  val window = findWindow(view) ?: return false
  return !isDecorFitsSystemWindows(window)
}

/**
 * Finds the window associated with the given view.
 */
private fun findWindow(view: View): Window? {
  return view.context.findWindow()
}

/**
 * Checks if the window's decor fits system windows.
 * Returns true if decor fits system windows (NOT edge-to-edge),
 * false if edge-to-edge is enabled.
 */
private fun isDecorFitsSystemWindows(window: Window): Boolean {
  return if (Build.VERSION.SDK_INT >= 30) {
    window.decorView.fitsSystemWindows
  } else {
    // On older APIs, check using ViewCompat
    ViewCompat.getFitsSystemWindows(window.decorView)
  }
}

/**
 * Extension function to find the Window from a Context.
 */
private fun android.content.Context.findWindow(): Window? {
  var context = this
  while (context is android.content.ContextWrapper) {
    if (context is android.app.Activity) {
      return context.window
    }
    context = context.baseContext
  }
  return null
}
