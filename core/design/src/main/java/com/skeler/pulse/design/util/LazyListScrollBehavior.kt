package com.skeler.pulse.design.util

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

private const val DEFAULT_NEAR_END_BUFFER = 2
private const val DEFAULT_ANIMATION_DISTANCE_THRESHOLD = 24

/**
 * Returns true when the current viewport is already near the end of the list.
 */
fun LazyListState.isNearListEnd(buffer: Int = DEFAULT_NEAR_END_BUFFER): Boolean {
    val info = layoutInfo
    if (info.totalItemsCount == 0) return true

    val lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index ?: return true
    return lastVisibleIndex >= info.totalItemsCount - 1 - buffer
}

/**
 * Uses animated scrolling for short distances and immediate jumps for very long distances.
 */
suspend fun LazyListState.scrollToItemSmoothly(
    targetIndex: Int,
    animateDistanceThreshold: Int = DEFAULT_ANIMATION_DISTANCE_THRESHOLD,
) {
    if (targetIndex < 0) return

    val distance = abs(targetIndex - firstVisibleItemIndex)
    if (distance <= animateDistanceThreshold) {
        animateScrollToItem(targetIndex)
    } else {
        scrollToItem(targetIndex)
    }
}
