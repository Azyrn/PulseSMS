package com.skeler.pulse.design.util

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private const val OVERSCROLL_MAX_OFFSET_PX = 280f
private const val OVERSCROLL_STRETCH_FACTOR = 0.42f

@Composable
fun rememberMomentumFlingBehavior(
    enabled: Boolean,
    momentumMultiplier: Float = 1.12f,
): FlingBehavior {
    val baseFlingBehavior = ScrollableDefaults.flingBehavior()
    if (!enabled) return baseFlingBehavior

    return remember(baseFlingBehavior, momentumMultiplier) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val boostedVelocity = initialVelocity * momentumMultiplier
                if (abs(boostedVelocity) <= 1f) {
                    return initialVelocity
                }
                return with(baseFlingBehavior) {
                    this@performFling.performFling(boostedVelocity)
                }
            }
        }
    }
}

@Composable
fun rememberSmoothFlingBehavior(
    enabled: Boolean,
    momentumMultiplier: Float = 1.12f,
): FlingBehavior = rememberMomentumFlingBehavior(
    enabled = enabled,
    momentumMultiplier = momentumMultiplier,
)

@Composable
fun Modifier.elasticOverscroll(
    enabled: Boolean,
    state: LazyListState,
    orientation: Orientation = Orientation.Vertical,
    reverseLayout: Boolean = false,
): Modifier {
    if (!enabled) return this

    var overscrollOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var reboundJob by remember { mutableStateOf<Job?>(null) }
    val resetSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        )
    }
    val startRebound = remember(resetSpec, scope) {
        {
            if (abs(overscrollOffset) > 0.5f) {
                reboundJob?.cancel()
                reboundJob = scope.launch {
                    AnimationState(initialValue = overscrollOffset).animateTo(
                        targetValue = 0f,
                        animationSpec = resetSpec,
                    ) {
                        overscrollOffset = value
                    }
                }
            }
        }
    }

    val connection = remember(state, reverseLayout, startRebound) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                reboundJob?.cancel()
                reboundJob = null

                val delta = if (orientation == Orientation.Vertical) available.y else available.x
                if (delta == 0f) return Offset.Zero
                if (overscrollOffset == 0f || sign(delta) == sign(overscrollOffset)) return Offset.Zero

                // Consume opposite-direction drag first to relax any existing stretch before
                // sending deltas back to the LazyColumn.
                val relaxation = sign(delta) * minOf(abs(delta), abs(overscrollOffset))
                overscrollOffset -= relaxation
                return if (orientation == Orientation.Vertical) Offset(0f, relaxation) else Offset(relaxation, 0f)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val axisVelocity = if (orientation == Orientation.Vertical) available.y else available.x
                if (abs(axisVelocity) < 1f && abs(overscrollOffset) > 0.5f) {
                    startRebound()
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                startRebound()
                return Velocity.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    reboundJob?.cancel()
                    reboundJob = null
                }
                val axisUnconsumed = if (orientation == Orientation.Vertical) available.y else available.x
                if (axisUnconsumed == 0f) return Offset.Zero

                // Only unconsumed drag at content boundaries should accumulate elastic stretch.
                val stretch = axisUnconsumed * OVERSCROLL_STRETCH_FACTOR
                overscrollOffset = (overscrollOffset + stretch).coerceIn(
                    -OVERSCROLL_MAX_OFFSET_PX,
                    OVERSCROLL_MAX_OFFSET_PX,
                )
                return if (orientation == Orientation.Vertical) Offset(0f, stretch) else Offset(stretch, 0f)
            }
        }
    }

    return this
        .nestedScroll(connection)
        .graphicsLayer {
            if (orientation == Orientation.Vertical) {
                translationY = overscrollOffset
            } else {
                translationX = overscrollOffset
            }
        }
}

@Composable
fun LazyItemScope.motionAnimateItemModifier(reducedMotion: Boolean): Modifier {
    return if (reducedMotion) {
        Modifier
    } else {
        Modifier.animateItem(
            fadeInSpec = tween(durationMillis = 250),
            fadeOutSpec = tween(durationMillis = 120),
            placementSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            ),
        )
    }
}

@Composable
fun rememberEntranceModifier(
    entranceKey: Any,
    reducedMotion: Boolean,
): Modifier {
    if (reducedMotion) return Modifier

    val density = LocalDensity.current
    val entranceOffsetPx = remember(density) { with(density) { 24.dp.toPx() } }
    var entered by remember(entranceKey) { mutableStateOf(false) }
    LaunchedEffect(entranceKey) {
        entered = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "list_item_alpha",
    )

    val translationY by animateFloatAsState(
        targetValue = if (entered) 0f else entranceOffsetPx,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy,
        ),
        label = "list_item_translation",
    )

    return Modifier.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    LocalAccessibilityManager.current
    val context = LocalContext.current
    var reducedMotionEnabled by remember(context) { mutableStateOf(isAnimationsDisabled(context)) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotionEnabled = isAnimationsDisabled(context)
            }
        }

        val resolver = context.contentResolver
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE),
            false,
            observer,
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.WINDOW_ANIMATION_SCALE),
            false,
            observer,
        )
        reducedMotionEnabled = isAnimationsDisabled(context)

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    return reducedMotionEnabled
}

private fun isAnimationsDisabled(context: Context): Boolean {
    return try {
        val transitionScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f,
        )
        val animatorScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        val windowScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.WINDOW_ANIMATION_SCALE,
            1f,
        )
        transitionScale == 0f || animatorScale == 0f || windowScale == 0f
    } catch (_: Throwable) {
        false
    }
}
