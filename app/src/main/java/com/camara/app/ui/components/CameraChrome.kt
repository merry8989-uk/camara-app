package com.camara.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.ui.theme.Accent

/** Round translucent icon button used across the camera chrome. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tint: Color = Color.White,
    background: Color = Color(0x66000000)
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (selected) Accent else background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Small selectable text pill (mode strip, extension strip). */
@Composable
fun PillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Accent.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = if (selected) Accent else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/** Rule-of-thirds grid drawn over the preview. */
@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = 1f
        val line = Color.White.copy(alpha = 0.35f)
        val w = size.width
        val h = size.height
        drawLine(line, Offset(w / 3f, 0f), Offset(w / 3f, h), stroke)
        drawLine(line, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), stroke)
        drawLine(line, Offset(0f, h / 3f), Offset(w, h / 3f), stroke)
        drawLine(line, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), stroke)
    }
}
