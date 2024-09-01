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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Surface
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleBottomSheetPopup
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.Scrim
import com.skydoves.flexible.core.consumeSwipeWithinBottomSheetBoundsNestedScrollConnection
import com.skydoves.flexible.core.emptySwipeWithinBottomSheetBoundsNestedScrollConnection
import com.skydoves.flexible.core.flexibleBottomSheetAnchorChangeHandler
import com.skydoves.flexible.core.flexibleBottomSheetSwipeable
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import com.skydoves.flexible.core.screenHeight
import com.skydoves.flexible.core.sheetPaddings
import com.skydoves.flexible.core.toPx
import kotlinx.coroutines.launch

/**
 * Flexible bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, flexible bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a flexible bottom sheet looks like this:
 *
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 * animates to [FlexibleSheetValue.Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param onTargetChanges Callback to listen for changes in [FlexibleSheetValue] targets.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 * the matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param windowInsets window insets to be passed to the bottom sheet window via [PaddingValues]
 * params.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
public fun FlexibleBottomSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  sheetState: FlexibleSheetState = rememberFlexibleBottomSheetState(),
  onTargetChanges: (FlexibleSheetValue) -> Unit = {},
  shape: Shape = BottomSheetDefaults.FullyExpandedShape,
  containerColor: Color = BottomSheetDefaults.ContainerColor,
  contentColor: Color = contentColorFor(containerColor),
  scrimColor: Color = BottomSheetDefaults.ScrimColor,
  dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
  windowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
  content: @Composable ColumnScope.() -> Unit,
) {
  val scope = rememberCoroutineScope()
  val animateToDismiss: () -> Unit = {
    if (sheetState.swipeableState.confirmValueChange(FlexibleSheetValue.Hidden)) {
      scope.launch { sheetState.hide() }.invokeOnCompletion {
        if (!sheetState.isVisible) {
          onDismissRequest()
        }
      }
    }
  }
  val settleToDismiss: (velocity: Float) -> Unit = {
    scope.launch { sheetState.settle(it) }.invokeOnCompletion {
      if (!sheetState.isVisible) onDismissRequest()
    }
  }

  // Callback that is invoked when the anchors have changed.
  val anchorChangeHandler = remember(sheetState, scope) {
    flexibleBottomSheetAnchorChangeHandler(
      state = sheetState,
      animateTo = { target, velocity ->
        scope.launch { sheetState.animateTo(target, velocity = velocity) }
      },
      snapTo = { target ->
        val didSnapImmediately = sheetState.trySnapTo(target)
        if (!didSnapImmediately) {
          scope.launch { sheetState.snapTo(target) }
        }
      },
    )
  }

  LaunchedEffect(sheetState.targetValue) {
    onTargetChanges.invoke(sheetState.targetValue)
  }

  LaunchedEffect(sheetState.swipeableState.anchors) {
    sheetState.swipeableState.isInitialized = sheetState.swipeableState.anchors.size ==
      listOf(
        FlexibleSheetValue.Hidden,
        FlexibleSheetValue.SlightlyExpanded,
        FlexibleSheetValue.IntermediatelyExpanded,
        FlexibleSheetValue.FullyExpanded,
      ).size
  }

  FlexibleBottomSheetPopup(
    onDismissRequest = {
      if (sheetState.currentValue == FlexibleSheetValue.FullyExpanded &&
        sheetState.hasIntermediatelyExpandedState
      ) {
        scope.launch { sheetState.intermediatelyExpand() }
      } else if (sheetState.currentValue == FlexibleSheetValue.IntermediatelyExpanded &&
        sheetState.hasSlightlyExpandedState
      ) {
        scope.launch { sheetState.slightlyExpand() }
      } else { // Is expanded without collapsed state or is collapsed.
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
      }
    },
    sheetState = sheetState,
    windowInsets = windowInsets,
  ) {
    var isDragging by remember { mutableStateOf(false) }
    val isAnimationRunning = sheetState.swipeableState.isAnimationRunning
    val screenHeightSize = screenHeight()
    val screenHeightPxSize = screenHeightSize.toPx()
    val fullyExpandedHeight: Dp = screenHeightSize * sheetState.flexibleSheetSize.fullyExpanded

    val flexibleSheetSize = sheetState.flexibleSheetSize
    val expectedSheetSize: Dp = when (sheetState.targetValue) {
      FlexibleSheetValue.Hidden -> 1.dp

      FlexibleSheetValue.FullyExpanded -> screenHeightSize * flexibleSheetSize.fullyExpanded

      FlexibleSheetValue.IntermediatelyExpanded ->
        screenHeightSize * flexibleSheetSize.intermediatelyExpanded

      FlexibleSheetValue.SlightlyExpanded -> screenHeightSize * flexibleSheetSize.slightlyExpanded
    }

    val sheetModifier = if (sheetState.isModal) {
      Modifier.fillMaxSize()
    } else {
      Modifier.height(
        if (isDragging || isAnimationRunning) {
          fullyExpandedHeight
        } else {
          expectedSheetSize
        },
      )
    }

    BoxWithConstraints(
      modifier = sheetModifier
        .align(Alignment.BottomCenter)
        .graphicsLayer {
          alpha =
            if (sheetState.targetValue ==
              FlexibleSheetValue.Hidden && !isDragging && !isAnimationRunning
            ) {
              0f
            } else {
              1f
            }
        },
    ) {
      val constraintHeight = constraints.maxHeight.toFloat()
      if (sheetState.isModal) {
        Scrim(
          color = scrimColor,
          onDismissRequest = animateToDismiss,
          visible = sheetState.targetValue != FlexibleSheetValue.Hidden,
        )
      }
      val bottomSheetPaneTitle = "Bottom Sheet"
      Surface(
        modifier = modifier
          .widthIn(max = BottomSheetMaxWidth)
          .fillMaxWidth()
          .fillMaxHeight()
          .align(Alignment.BottomCenter)
          .semantics { paneTitle = bottomSheetPaneTitle }
          .offset {
            val offset = sheetState
              .requireOffset()
              .toInt()

            IntOffset(
              x = 0,
              y = if (sheetState.isModal) {
                offset
              } else {
                if (isDragging || isAnimationRunning) {
                  offset
                } else {
                  0
                }
              },
            )
          }
          .nestedScroll(
            remember(sheetState) {
              if (sheetState.allowNestedScroll) {
                consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                  sheetState = sheetState,
                  orientation = Orientation.Vertical,
                  screenHeight = screenHeightSize.value,
                  onFling = settleToDismiss,
                  onDragging = {
                    isDragging = it
                  },
                )
              } else {
                emptySwipeWithinBottomSheetBoundsNestedScrollConnection()
              }
            },
          )
          .flexibleBottomSheetSwipeable(
            sheetState = sheetState,
            anchorChangeHandler = anchorChangeHandler,
            sheetFullHeight = fullyExpandedHeight.toPx(),
            sheetConstraintHeight = constraintHeight,
            screenMaxHeight = screenHeightSize.toPx(),
            flexibleSheetSize = sheetState.flexibleSheetSize,
            isModal = sheetState.isModal,
            onDragStarted = {
              isDragging = true
            },
            onDragStopped = {
              isDragging = false
              settleToDismiss(it)
            },
          ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
      ) {
        Column(
          modifier = if (sheetState.isModal) {
            Modifier
              .fillMaxWidth()
              .sheetPaddings(sheetState)
          } else {
            Modifier
              .fillMaxWidth()
          },
        ) {
          if (dragHandle != null) {
            val collapseActionLabel = "Collapse bottom sheet"
            val dismissActionLabel = "Dismiss bottom sheet"
            val expandActionLabel = "expand bottom sheet"
            Box(
              modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .semantics(mergeDescendants = true) {
                  // Provides semantics to interact with the bottomsheet based on its
                  // current value.
                  with(sheetState) {
                    dismiss(dismissActionLabel) {
                      animateToDismiss()
                      true
                    }
                    if (currentValue == FlexibleSheetValue.IntermediatelyExpanded) {
                      expand(expandActionLabel) {
                        if (swipeableState.confirmValueChange(
                            FlexibleSheetValue.FullyExpanded,
                          )
                        ) {
                          scope.launch { fullyExpand() }
                        }
                        true
                      }
                    } else if (currentValue == FlexibleSheetValue.SlightlyExpanded) {
                      expand(expandActionLabel) {
                        if (swipeableState.confirmValueChange(
                            FlexibleSheetValue.IntermediatelyExpanded,
                          )
                        ) {
                          scope.launch { intermediatelyExpand() }
                        }
                        true
                      }
                    } else if (hasIntermediatelyExpandedState) {
                      collapse(collapseActionLabel) {
                        if (
                          swipeableState.confirmValueChange(
                            FlexibleSheetValue.IntermediatelyExpanded,
                          )
                        ) {
                          scope.launch { intermediatelyExpand() }
                        }
                        true
                      }
                    } else if (hasSlightlyExpandedState) {
                      collapse(collapseActionLabel) {
                        if (
                          swipeableState.confirmValueChange(
                            FlexibleSheetValue.SlightlyExpanded,
                          )
                        ) {
                          scope.launch { slightlyExpand() }
                        }
                        true
                      }
                    }
                  }
                },
            ) {
              dragHandle()
            }
          }
          content()
        }
      }
    }
  }
  if (sheetState.hasFullyExpandedState) {
    LaunchedEffect(sheetState) {
      sheetState.show()
    }
  }
}
