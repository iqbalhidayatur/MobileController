package com.mobilegamecontroller.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Virtual analog stick supporting 360° movement.
 * Outputs normalized X/Y values in range -1.0 to 1.0.
 */
@Composable
fun VirtualAnalogStick(
    onMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
    outerSize: Dp = 140.dp,
    innerSize: Dp = 56.dp,
    vibrationEnabled: Boolean = true
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val outerRadiusPx = with(density) { (outerSize / 2).toPx() }
    val innerRadiusPx = with(density) { (innerSize / 2).toPx() }
    val maxDragPx = outerRadiusPx - innerRadiusPx

    var knobOffset by remember { mutableStateOf(IntOffset.Zero) }
    var hasVibrated by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(outerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
        )

        // Draggable knob
        Box(
            modifier = Modifier
                .offset { knobOffset }
                .size(innerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            knobOffset = IntOffset.Zero
                            onMove(0f, 0f)
                            hasVibrated = false
                        },
                        onDragCancel = {
                            knobOffset = IntOffset.Zero
                            onMove(0f, 0f)
                            hasVibrated = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val newX = (knobOffset.x + dragAmount.x).coerceIn(-maxDragPx, maxDragPx)
                        val newY = (knobOffset.y + dragAmount.y).coerceIn(-maxDragPx, maxDragPx)

                        // Clamp to circular boundary
                        val dist = hypot(newX.toDouble(), newY.toDouble()).toFloat()
                        val (clampedX, clampedY) = if (dist > maxDragPx) {
                            val scale = maxDragPx / dist
                            newX * scale to newY * scale
                        } else {
                            newX to newY
                        }

                        knobOffset = IntOffset(clampedX.roundToInt(), clampedY.roundToInt())

                        val normX = (clampedX / maxDragPx).coerceIn(-1f, 1f)
                        val normY = -(clampedY / maxDragPx).coerceIn(-1f, 1f) // Invert Y for game coords

                        if (vibrationEnabled && !hasVibrated && (normX != 0f || normY != 0f)) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            hasVibrated = true
                        }

                        onMove(normX, normY)
                    }
                }
        )
    }
}
