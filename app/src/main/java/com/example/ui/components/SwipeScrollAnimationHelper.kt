package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput

enum class SlideBarPosition {
    TOP,
    BOTTOM
}

/**
 * State holding bar visibility and handling nested scrolling and swipe drag gestures.
 */
@Stable
class ScrollVisibilityState(
    initialVisible: Boolean = true,
    private val scrollThresholdPx: Float = 12f
) {
    var isVisible by mutableStateOf(initialVisible)
        private set

    fun show() {
        if (!isVisible) isVisible = true
    }

    fun hide() {
        if (isVisible) isVisible = false
    }

    fun toggle() {
        isVisible = !isVisible
    }

    /**
     * NestedScrollConnection that observes list scroll events.
     * Scrolling down hides the bar; scrolling up restores it.
     */
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            if (delta < -scrollThresholdPx) {
                // Scrolling down -> swipe down content -> hide bar
                hide()
            } else if (delta > scrollThresholdPx) {
                // Scrolling up -> swipe up content -> show bar
                show()
            }
            return Offset.Zero
        }
    }
}

/**
 * Remembers a ScrollVisibilityState instance that can be attached to scrollable containers.
 */
@Composable
fun rememberScrollVisibilityState(
    initialVisible: Boolean = true,
    scrollThresholdPx: Float = 14f
): ScrollVisibilityState {
    return remember {
        ScrollVisibilityState(
            initialVisible = initialVisible,
            scrollThresholdPx = scrollThresholdPx
        )
    }
}

/**
 * Modifier extension to detect direct vertical swipe gestures for quick hide/show.
 */
fun Modifier.swipeToHideShow(
    state: ScrollVisibilityState,
    dragThresholdPx: Float = 18f
): Modifier = this.pointerInput(Unit) {
    detectVerticalDragGestures { _, dragAmount ->
        if (dragAmount < -dragThresholdPx) {
            // Dragged down
            state.hide()
        } else if (dragAmount > dragThresholdPx) {
            // Dragged up
            state.show()
        }
    }
}

/**
 * Reusable animated wrapper for Top, Bottom, or Overlay bars with smooth slide & fade transitions.
 */
@Composable
fun AnimatedSlideBar(
    visible: Boolean,
    position: SlideBarPosition = SlideBarPosition.BOTTOM,
    modifier: Modifier = Modifier,
    animationDurationMs: Int = 300,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = when (position) {
            SlideBarPosition.TOP -> slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing))

            SlideBarPosition.BOTTOM -> slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing))
        },
        exit = when (position) {
            SlideBarPosition.TOP -> slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = animationDurationMs))

            SlideBarPosition.BOTTOM -> slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = animationDurationMs))
        }
    ) {
        content()
    }
}
