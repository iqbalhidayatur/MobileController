package com.mobilegamecontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilegamecontroller.domain.model.AnalogStick
import com.mobilegamecontroller.domain.model.ControllerButton
import com.mobilegamecontroller.ui.components.ConnectionStatusBadge
import com.mobilegamecontroller.ui.components.DPad
import com.mobilegamecontroller.ui.components.GameButton
import com.mobilegamecontroller.ui.components.ShoulderButton
import com.mobilegamecontroller.ui.components.VirtualAnalogStick
import com.mobilegamecontroller.viewmodel.ControllerViewModel

/**
 * Full-screen landscape controller layout.
 * Responsive: uses weight-based layout for different screen sizes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ControllerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val vibration = uiState.settings.vibrationEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Controller") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect(onNavigateBack)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ConnectionStatusBadge(
                        connectionState = uiState.connectionState,
                        latencyMs = uiState.latencyMs,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: LB, LT, Left analog, D-Pad
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ShoulderButton(
                            label = "LB",
                            onPress = { viewModel.onButtonEvent(ControllerButton.LB, true) },
                            onRelease = { viewModel.onButtonEvent(ControllerButton.LB, false) },
                            modifier = Modifier.width(64.dp).height(36.dp),
                            vibrationEnabled = vibration
                        )
                        ShoulderButton(
                            label = "LT",
                            onPress = { viewModel.onButtonEvent(ControllerButton.LT, true) },
                            onRelease = { viewModel.onButtonEvent(ControllerButton.LT, false) },
                            modifier = Modifier.width(64.dp).height(36.dp),
                            vibrationEnabled = vibration
                        )
                    }

                    VirtualAnalogStick(
                        onMove = { x, y -> viewModel.onAnalogEvent(AnalogStick.LEFT, x, y) },
                        outerSize = 130.dp,
                        vibrationEnabled = vibration
                    )

                    DPad(
                        onButtonPress = { viewModel.onButtonEvent(it, true) },
                        onButtonRelease = { viewModel.onButtonEvent(it, false) },
                        size = 110.dp,
                        vibrationEnabled = vibration
                    )
                }

                // Center: Start / Select
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameButton(
                        label = "Select",
                        onPress = { viewModel.onButtonEvent(ControllerButton.SELECT, true) },
                        onRelease = { viewModel.onButtonEvent(ControllerButton.SELECT, false) },
                        size = 44.dp,
                        vibrationEnabled = vibration
                    )
                    GameButton(
                        label = "Start",
                        onPress = { viewModel.onButtonEvent(ControllerButton.START, true) },
                        onRelease = { viewModel.onButtonEvent(ControllerButton.START, false) },
                        size = 44.dp,
                        vibrationEnabled = vibration
                    )
                }

                // Right side: RB, RT, Right analog, ABXY
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ShoulderButton(
                            label = "RB",
                            onPress = { viewModel.onButtonEvent(ControllerButton.RB, true) },
                            onRelease = { viewModel.onButtonEvent(ControllerButton.RB, false) },
                            modifier = Modifier.width(64.dp).height(36.dp),
                            vibrationEnabled = vibration
                        )
                        ShoulderButton(
                            label = "RT",
                            onPress = { viewModel.onButtonEvent(ControllerButton.RT, true) },
                            onRelease = { viewModel.onButtonEvent(ControllerButton.RT, false) },
                            modifier = Modifier.width(64.dp).height(36.dp),
                            vibrationEnabled = vibration
                        )
                    }

                    VirtualAnalogStick(
                        onMove = { x, y -> viewModel.onAnalogEvent(AnalogStick.RIGHT, x, y) },
                        outerSize = 130.dp,
                        vibrationEnabled = vibration
                    )

                    // Diamond ABXY layout
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GameButton(
                            label = "Y",
                            onPress = { viewModel.onButtonEvent(ControllerButton.Y, true) },
                            onRelease = { viewModel.onButtonEvent(ControllerButton.Y, false) },
                            size = 46.dp,
                            vibrationEnabled = vibration
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                            GameButton(
                                label = "X",
                                onPress = { viewModel.onButtonEvent(ControllerButton.X, true) },
                                onRelease = { viewModel.onButtonEvent(ControllerButton.X, false) },
                                size = 46.dp,
                                vibrationEnabled = vibration
                            )
                            GameButton(
                                label = "B",
                                onPress = { viewModel.onButtonEvent(ControllerButton.B, true) },
                                onRelease = { viewModel.onButtonEvent(ControllerButton.B, false) },
                                size = 46.dp,
                                vibrationEnabled = vibration
                            )
                        }
                        GameButton(
                            label = "A",
                            onPress = { viewModel.onButtonEvent(ControllerButton.A, true) },
                            onRelease = { viewModel.onButtonEvent(ControllerButton.A, false) },
                            size = 46.dp,
                            vibrationEnabled = vibration
                        )
                    }
                }
            }
        }
    }
}
