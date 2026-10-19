package com.camara.app.editor.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Freehand drawing surface. [active] gates input so the layer only captures
 * strokes when the Draw tool is selected. Points are stored normalised so they
 * bake correctly at export resolution.
 */
@Composable
fun DrawLayer(
    strokes: List<DrawStroke>,
    active: Boolean,
    colorArgb: Int,
    widthFraction: Float,
    onAddStroke: (DrawStroke) -> Unit,
    modifier: Modifier = Modifier
) {
    var current by remember { mutableStateOf<List<Pt>>(emptyList()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(active, colorArgb, widthFraction) {
                if (!active) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        current = listOf(normalize(offset, size.width.toFloat(), size.height.toFloat()))
                    },
                    onDragEnd = {
                        if (current.size >= 2) {
                            onAddStroke(DrawStroke(current, colorArgb, widthFraction))
                        }
                        current = emptyList()
                    },
                    onDragCancel = { current = emptyList() },
                    onDrag = { change, _ ->
                        current = current + normalize(
                            change.position,
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
                    }
                )
            }
    ) {
        val all = if (current.isEmpty()) strokes else strokes + DrawStroke(current, colorArgb, widthFraction)
        all.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path().apply {
                val first = stroke.points.first()
                moveTo(first.x * size.width, first.y * size.height)
                stroke.points.drop(1).forEach { p ->
                    lineTo(p.x * size.width, p.y * size.height)
                }
            }
            drawPath(
                path = path,
                color = Color(stroke.colorArgb),
                style = Stroke(
                    width = (stroke.widthFraction * minOf(size.width, size.height)).coerceAtLeast(2f)
                )
            )
        }
    }
}

private fun normalize(offset: Offset, w: Float, h: Float): Pt =
    Pt((offset.x / w).coerceIn(0f, 1f), (offset.y / h).coerceIn(0f, 1f))
