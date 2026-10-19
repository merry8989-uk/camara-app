package com.camara.app.editor.image

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
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

/** Draggable / scalable / rotatable text overlays for the image editor. */
@Composable
fun TextItemLayer(
    texts: List<TextItem>,
    onUpdate: (TextItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        var selectedId by remember { mutableStateOf<Long?>(null) }

        texts.forEach { item ->
            val selected = item.id == selectedId
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = item.x * widthPx - widthPx / 2f
                        translationY = item.y * heightPx - heightPx / 2f
                        rotationZ = item.rotation
                    }
                    .align(Alignment.Center)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                    .pointerInput(item.id) {
                        detectTransformGestures { _, pan, zoom, rotate ->
                            selectedId = item.id
                            onUpdate(
                                item.copy(
                                    x = (item.x + pan.x / widthPx).coerceIn(0.02f, 0.98f),
                                    y = (item.y + pan.y / heightPx).coerceIn(0.02f, 0.98f),
                                    scale = (item.scale * zoom).coerceIn(0.3f, 4f),
                                    rotation = item.rotation + rotate
                                )
                            )
                        }
                    }
            ) {
                Text(
                    text = item.text,
                    color = Color(item.colorArgb),
                    fontSize = (item.sizeFraction * 220f * item.scale).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
