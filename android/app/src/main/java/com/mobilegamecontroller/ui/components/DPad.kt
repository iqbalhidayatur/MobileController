package com.mobilegamecontroller.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilegamecontroller.domain.model.ControllerButton

/**
 * Digital D-Pad with four directional buttons arranged in cross pattern.
 */
@Composable
fun DPad(
    onButtonPress: (ControllerButton) -> Unit,
    onButtonRelease: (ControllerButton) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    vibrationEnabled: Boolean = true
) {
    val view = LocalView.current
    val buttonSize = size * 0.35f
    val centerOffset = size * 0.325f

    Box(modifier = modifier.size(size)) {
        DPadButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp),
            size = buttonSize,
            vibrationEnabled = vibrationEnabled,
            onPress = {
                if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onButtonPress(ControllerButton.DPAD_UP)
            },
            onRelease = { onButtonRelease(ControllerButton.DPAD_UP) }
        )
        DPadButton(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            size = buttonSize,
            vibrationEnabled = vibrationEnabled,
            onPress = {
                if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onButtonPress(ControllerButton.DPAD_DOWN)
            },
            onRelease = { onButtonRelease(ControllerButton.DPAD_DOWN) }
        )
        DPadButton(
            modifier = Modifier
                .align(Alignment.CenterStart),
            size = buttonSize,
            vibrationEnabled = vibrationEnabled,
            onPress = {
                if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onButtonPress(ControllerButton.DPAD_LEFT)
            },
            onRelease = { onButtonRelease(ControllerButton.DPAD_LEFT) }
        )
        DPadButton(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            size = buttonSize,
            vibrationEnabled = vibrationEnabled,
            onPress = {
                if (vibrationEnabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onButtonPress(ControllerButton.DPAD_RIGHT)
            },
            onRelease = { onButtonRelease(ControllerButton.DPAD_RIGHT) }
        )
        // Center indicator
        Box(
            modifier = Modifier
                .size(buttonSize * 0.6f)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun DPadButton(
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    vibrationEnabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            }
    )
}
