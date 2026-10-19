package com.camara.app.stickers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.Text

/**
 * Renders and edits a list of [PlacedSticker]s over whatever is behind it.
 * Tap to select; drag to move, pinch to scale, twist to rotate. The selected
 * sticker shows a delete button.
 */
@Composable
fun StickerLayer(
    stickers: List<PlacedSticker>,
    onUpdate: (PlacedSticker) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    baseSize: TextUnit = 48.sp
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        var selectedId by remember { mutableStateOf<Long?>(null) }

        stickers.forEach { sticker ->
            val selected = sticker.id == selectedId
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = sticker.x * widthPx - widthPx / 2f
                        translationY = sticker.y * heightPx - heightPx / 2f
                        scaleX = sticker.scale
                        scaleY = sticker.scale
                        rotationZ = sticker.rotation
                    }
                    .align(Alignment.Center)
                    .pointerInput(sticker.id) {
                        detectTransformGestures { _, pan, zoom, rotate ->
                            selectedId = sticker.id
                            onUpdate(
                                sticker.copy(
                                    x = (sticker.x + pan.x / widthPx).coerceIn(0.02f, 0.98f),
                                    y = (sticker.y + pan.y / heightPx).coerceIn(0.02f, 0.98f),
                                    scale = (sticker.scale * zoom).coerceIn(0.3f, 5f),
                                    rotation = sticker.rotation + rotate
                                )
                            )
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (selected) Modifier
                                .border(BorderStroke(1.5.dp, Color.White), CircleShape)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            else Modifier
                        )
                        .clip(CircleShape)
                ) {
                    Text(
                        text = sticker.emoji,
                        fontSize = baseSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .graphicsLayer { translationX = 10f; translationY = -10f }
                            .clip(CircleShape)
                            .background(Color(0xCC000000))
                            .clickable { onDelete(sticker.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove sticker",
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer { scaleX = 0.6f; scaleY = 0.6f }
                        )
                    }
                }
            }
        }
    }
}
