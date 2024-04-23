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
package com.skydoves.flexible.bottomsheet.material

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Contains the default values used by [FlexibleBottomSheet].
 */
@Stable
public object BottomSheetDefaults {
  /** The default shape for bottom sheets in a [FlexibleSheetValue.Hidden] state. */
  public val HiddenShape: Shape
    @Composable get() = SheetBottomTokens.DockedMinimizedContainerShape

  /** The default shape for a bottom sheets in [FlexibleSheetValue.IntermediatelyExpanded] and [FlexibleSheetValue.FullyExpanded] states. */
  public val FullyExpandedShape: Shape
    @Composable get() = RoundedCornerShape(
      topStart = 28.0.dp,
      topEnd = 28.0.dp,
      bottomEnd = 0.0.dp,
      bottomStart = 0.0.dp,
    )

  /** The default container color for a bottom sheet. */
  public val ContainerColor: Color
    @Composable get() = MaterialTheme.colors.surface

  /** The default elevation for a bottom sheet. */
  public val Elevation: Dp = SheetBottomTokens.DockedModalContainerElevation

  /** The default color of the scrim overlay for background content. */
  public val ScrimColor: Color
    @Composable get() = if (isSystemInDarkTheme()) {
      Color.Black.copy(alpha = 0.32f)
    } else {
      Color.White.copy(alpha = 0.32f)
    }

  /**
   * Default insets to be used and consumed by the [FlexibleBottomSheet] window.
   */
  public val windowInsets: WindowInsets
    @Composable
    get() = WindowInsets.systemBars.only(WindowInsetsSides.Vertical)

  /**
   * The optional visual marker placed on top of a bottom sheet to indicate it may be dragged.
   */
  @Composable
  public fun DragHandle(
    modifier: Modifier = Modifier,
    width: Dp = SheetBottomTokens.DockedDragHandleWidth,
    height: Dp = SheetBottomTokens.DockedDragHandleHeight,
    shape: Shape = RoundedCornerShape(28.0.dp),
    verticalPadding: Dp = DragHandleVerticalPadding,
    color: Color = MaterialTheme.colors.secondaryVariant
      .copy(SheetBottomTokens.DockedDragHandleOpacity),
  ) {
    Surface(
      modifier = modifier
        .padding(vertical = verticalPadding)
        .semantics { contentDescription = "DragHandle" },
      color = color,
      shape = shape,
    ) {
      Box(
        Modifier
          .size(
            width = width,
            height = height,
          ),
      )
    }
  }
}

internal object SheetBottomTokens {
  val DockedMinimizedContainerShape = RectangleShape
  val DockedModalContainerElevation = 1.0.dp
  val DockedDragHandleHeight = 4.0.dp
  const val DockedDragHandleOpacity = 0.4f
  val DockedDragHandleWidth = 32.0.dp
}

private val DragHandleVerticalPadding = 22.dp
internal val BottomSheetMaxWidth = 640.dp
