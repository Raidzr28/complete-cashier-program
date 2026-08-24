package com.rzk.kasirpro.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

/**
 * Interior motion & texture helpers — the details that make a flat repaint feel like a
 * considered, physical shop shelf rather than a colour swap. Kept in one file since none
 * of these belong to a single screen; every screen borrows from here.
 */

/**
 * Soft press feedback for anything tappable: a barely-there scale-down that reads as
 * "the shelf gave a little" rather than a hard state flip. Shares its interaction source
 * with the caller so ripple and scale animate off the same press signal.
 */
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.97f): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f),
        label = "pressScale"
    )
    return scale
}

/**
 * Fades and lifts content in, staggered by [index] — the cascade that makes a screen feel
 * poured out rather than slammed on. Plays once, the first time this composable enters the
 * tree (a fresh screen load, or a lazy row scrolling into view for the first time).
 *
 * Kept quick and subtle on purpose: only the first handful of items get a meaningful delay
 * (past that, a long list scrolling into view would otherwise feel like it's perpetually
 * still animating rather than just being there), and each item settles fast enough that
 * cascading never reads as sluggish.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { state.targetState = true }
    val delay = (index * 20).coerceAtMost(120)
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(160, delayMillis = delay)) +
            slideInVertically(tween(160, delayMillis = delay)) { it / 12 }
    ) { content() }
}

/**
 * A soft, hand-drawn-feeling glow made of overlapping circles rather than a perfect
 * gradient mesh — reads as organic instead of "generated". Purely decorative, so it draws
 * behind content and never intercepts touch.
 */
@Composable
fun OrganicBlob(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color.copy(alpha = 0.16f), radius = w * 0.32f, center = Offset(w * 0.78f, h * 0.22f))
        drawCircle(color.copy(alpha = 0.12f), radius = w * 0.22f, center = Offset(w * 0.92f, h * 0.55f))
        drawCircle(color.copy(alpha = 0.10f), radius = w * 0.16f, center = Offset(w * 0.55f, h * 0.08f))
    }
}

/**
 * Film-grain speckle so a flat fill reads as paper/shelf material instead of a solid
 * swatch. The noise bitmap is generated once per call site and cached for the life of the
 * composition — a one-time cost, never a per-frame one — then tiled via a shader brush and
 * composited with Overlay so mid-grey pixels stay neutral (true speckle, not a grey wash).
 */
fun Modifier.grainOverlay(alpha: Float = 0.05f): Modifier = composed {
    val brush = remember { buildGrainBrush() }
    drawWithContent {
        drawContent()
        drawRect(brush = brush, alpha = alpha, blendMode = BlendMode.Overlay)
    }
}

private fun buildGrainBrush(): Brush {
    val size = 128
    val pixels = IntArray(size * size)
    val random = Random(7)
    for (i in pixels.indices) {
        val v = random.nextInt(256)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}
