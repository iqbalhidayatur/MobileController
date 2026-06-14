package com.mobilegamecontroller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobilegamecontroller.domain.model.ConnectionState

@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (connectionState) {
        ConnectionState.CONNECTED -> "Connected" to Color(0xFF4CAF50)
        ConnectionState.CONNECTING -> "Connecting…" to Color(0xFFFFC107)
        ConnectionState.RECONNECTING -> "Reconnecting…" to Color(0xFFFF9800)
        ConnectionState.DISCONNECTED -> "Disconnected" to Color(0xFFF44336)
        ConnectionState.ERROR -> "Error" to Color(0xFFF44336)
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "●",
            color = color,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium
        )
        if (connectionState == ConnectionState.CONNECTED && latencyMs >= 0) {
            Text(
                text = "  ${latencyMs}ms",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
