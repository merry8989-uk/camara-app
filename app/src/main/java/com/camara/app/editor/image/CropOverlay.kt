package com.camara.app.editor.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class Handle { NONE, TL, TR, BL, BR, L, R, T, B, MOVE }

/**
 * Interactive crop frame. Drag a corner or edge to resize, drag the middle to move.
 * When [aspectLock] is set (e.g. 1f for square) corner drags preserve the ratio.
 */
@Composable
fun CropOverlay(
    crop: CropRect,
    aspectLock: Float?,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val touchRadiusPx = with(LocalDensity.current) { 34.dp.toPx() }
    var handle by remember { mutableStateOf(Handle.NONE) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boxW = with(LocalDensity.current) { maxWidth.toPx() }
        val boxH = with(LocalDensity.current) { maxHeight.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(crop, aspectLock) {
                    detectDragGestures(
                        onDragStart = { start ->
                            handle = hitTest(start, crop, boxW, boxH, touchRadiusPx)
                        },
                        onDragEnd = { handle = Handle.NONE },
                        onDragCancel = { handle = Handle.NONE },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (handle == Handle.NONE) return@detectDragGestures
                            val dx = dragAmount.x / boxW
                            val dy = dragAmount.y / boxH
                            onCropChange(applyDrag(crop, handle, dx, dy, aspectLock))
                        }
                    )
                }
        ) {
            val r = Rect(
                crop.left * size.width,
                crop.top * size.height,
                crop.right * size.width,
                crop.bottom * size.height
            )
            val scrim = Color(0xB3000000)

            // Dim everything outside the crop rect.
            drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(size.width, r.top))
            drawRect(scrim, topLeft = Offset(0f, r.bottom), size = Size(size.width, size.height - r.bottom))
            drawRect(scrim, topLeft = Offset(0f, r.top), size = Size(r.left, r.height))
            drawRect(scrim, topLeft = Offset(r.right, r.top), size = Size(size.width - r.right, r.height))

            // Border + rule-of-thirds guides.
            drawRect(
                color = Color.White,
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
                style = Stroke(width = 2f)
            )
            val guide = Color.White.copy(alpha = 0.4f)
            for (i in 1..2) {
                val x = r.left + r.width * i / 3f
                val y = r.top + r.height * i / 3f
                drawLine(guide, Offset(x, r.top), Offset(x, r.bottom), 1.5f)
                drawLine(guide, Offset(r.left, y), Offset(r.right, y), 1.5f)
            }

            // Corner handles.
            val handleR = 11f
            listOf(
                Offset(r.left, r.top),
                Offset(r.right, r.top),
                Offset(r.left, r.bottom),
                Offset(r.right, r.bottom)
            ).forEach { c ->
                drawCircle(Color.White, radius = handleR, center = c)
                drawCircle(Color(0xFF0B0B0F), radius = handleR * 0.55f, center = c)
            }
        }
    }
}

private fun hitTest(p: Offset, crop: CropRect, w: Float, h: Float, radius: Float): Handle {
    val l = crop.left * w
    val t = crop.top * h
    val r = crop.right * w
    val b = crop.bottom * h

    fun near(x: Float, y: Float) = abs(p.x - x) <= radius && abs(p.y - y) <= radius

    return when {
        near(l, t) -> Handle.TL
        near(r, t) -> Handle.TR
        near(l, b) -> Handle.BL
        near(r, b) -> Handle.BR
        near(l, (t + b) / 2f) -> Handle.L
        near(r, (t + b) / 2f) -> Handle.R
        near((l + r) / 2f, t) -> Handle.T
        near((l + r) / 2f, b) -> Handle.B
        p.x > l && p.x < r && p.y > t && p.y < b -> Handle.MOVE
        else -> Handle.NONE
    }
}

private const val MIN_SIZE = 0.12f

private fun applyDrag(c: CropRect, handle: Handle, dx: Float, dy: Float, aspect: Float?): CropRect {
    if (handle == Handle.MOVE) {
        val l = (c.left + dx).coerceIn(0f, 1f - c.width)
        val t = (c.top + dy).coerceIn(0f, 1f - c.height)
        return CropRect(l, t, l + c.width, t + c.height)
    }

    var l = c.left
    var t = c.top
    var r = c.right
    var b = c.bottom

    when (handle) {
        Handle.TL -> { l += dx; t += dy }
        Handle.TR -> { r += dx; t += dy }
        Handle.BL -> { l += dx; b += dy }
        Handle.BR -> { r += dx; b += dy }
        Handle.L -> l += dx
        Handle.R -> r += dx
        Handle.T -> t += dy
        Handle.B -> b += dy
        else -> return c
    }

    l = l.coerceIn(0f, r - MIN_SIZE)
    t = t.coerceIn(0f, b - MIN_SIZE)
    r = r.coerceIn(l + MIN_SIZE, 1f)
    b = b.coerceIn(t + MIN_SIZE, 1f)

    val isCorner = handle == Handle.TL || handle == Handle.TR ||
        handle == Handle.BL || handle == Handle.BR

    if (aspect != null && isCorner) {
        val fixedX = if (handle == Handle.TL || handle == Handle.BL) r else l
        val fixedY = if (handle == Handle.TL || handle == Handle.TR) b else t
        val signX = if (handle == Handle.TL || handle == Handle.BL) -1f else 1f
        val signY = if (handle == Handle.TL || handle == Handle.TR) -1f else 1f

        var width = abs(r - l)
        var height = width / aspect

        // Keep inside the frame.
        val maxWidth = if (signX < 0) fixedX else 1f - fixedX
        val maxHeight = if (signY < 0) fixedY else 1f - fixedY
        if (height > maxHeight) {
            height = maxHeight
            width = height * aspect
        }
        width = min(width, maxWidth)
        height = min(height, maxHeight.coerceAtLeast(MIN_SIZE))
        width = max(width, MIN_SIZE)

        val nx = fixedX + signX * width
        val ny = fixedY + signY * height
        l = min(fixedX, nx)
        r = max(fixedX, nx)
        t = min(fixedY, ny)
        b = max(fixedY, ny)
    }

    return CropRect(l, t, r, b)
}
