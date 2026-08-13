package btm.m.todaywallpaper.ui.widget

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.RoundedCorner
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlin.math.min

@Stable
class MomentumBackState internal constructor() {
    var progress by mutableFloatStateOf(0f)
        internal set
    var isSwiping by mutableStateOf(false)
        internal set
    var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
        internal set
}

@Composable
fun rememberMomentumPredictiveBack(
    enabled: Boolean,
    maxProgressPercent: Int,
    onBack: () -> Unit,
    handlerEnabled: Boolean = true
): MomentumBackState {
    val state = remember { MomentumBackState() }
    val latestOnBack by rememberUpdatedState(onBack)

    BackHandler(enabled = handlerEnabled && !enabled) { latestOnBack() }
    PredictiveBackHandler(enabled = handlerEnabled && enabled) { events ->
        state.isSwiping = true
        var committed = false
        try {
            events.collect { event ->
                state.progress = min(event.progress, maxProgressPercent.coerceIn(10, 100) / 100f)
                state.swipeEdge = event.swipeEdge
            }
            // Keep the outgoing page in its completed position until Android removes
            // the Activity. Resetting here produces a visible snap-back before finish().
            committed = true
            state.progress = 1f
            latestOnBack()
        } catch (_: CancellationException) {
            state.progress = 0f
        } finally {
            if (!committed) {
                state.isSwiping = false
                state.progress = 0f
            }
        }
    }
    LaunchedEffect(handlerEnabled) {
        if (!handlerEnabled) {
            state.progress = 0f
            state.isSwiping = false
        }
    }
    return state
}

@Composable
fun rememberDeviceCornerRadius(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    var radius by remember { mutableStateOf(28.dp) }
    LaunchedEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            var insets = view.rootWindowInsets
            if (insets == null) {
                delay(100)
                insets = view.rootWindowInsets
            }
            val px = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
            if (px > 0) radius = with(density) { px.toDp() }
        }
    }
    return radius
}

/** Exact Momentum gesture transform: 12% scale, 48dp X, 16dp Y and device-corner reveal. */
@Composable
fun Modifier.momentumBackTransform(state: MomentumBackState): Modifier {
    val sign = if (state.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
    val progress = if (state.isSwiping) state.progress else 0f
    return momentumBackTransform(progress, sign)
}

/** Shared transform for screens that keep their own navigation state. */
@Composable
fun Modifier.momentumBackTransform(progress: Float, direction: Float): Modifier {
    val density = LocalDensity.current
    val deviceCorner = rememberDeviceCornerRadius()
    val normalizedProgress = progress.coerceIn(0f, 1f)
    val sign = if (direction < 0f) -1f else 1f
    val scale = 1f - normalizedProgress * 0.12f
    val x = sign * normalizedProgress * with(density) { 48.dp.toPx() }
    val y = normalizedProgress * with(density) { 16.dp.toPx() }
    val corner = deviceCorner * normalizedProgress
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = x
        translationY = y
    }.clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))
}

/** Makes a secondary Activity window transparent so its caller remains visible below the card. */
fun Activity.enableMomentumTransparentWindow() {
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    window.attributes = window.attributes.apply { dimAmount = 0f }
    window.setBackgroundDrawableResource(android.R.color.transparent)
    window.statusBarColor = AndroidColor.TRANSPARENT
    window.navigationBarColor = AndroidColor.TRANSPARENT
    window.setDimAmount(0f)
    window.decorView.setBackgroundColor(AndroidColor.TRANSPARENT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }
}