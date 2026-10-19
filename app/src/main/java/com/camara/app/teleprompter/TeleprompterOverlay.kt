package com.camara.app.teleprompter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.ui.theme.Accent
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A transparent, draggable teleprompter that sits *on top of the camera preview*,
 * so you can read your script and watch yourself at the same time.
 *
 * Drag the header to move it; use the controls to play/pause, change speed and text
 * size, mirror the text, cycle opacity, edit the script, or close the overlay.
 */
@Composable
fun TeleprompterOverlay(
    onEditScript: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { TeleprompterStore.load(context) }

    var playing by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val scroll = rememberScrollState()

    LaunchedEffect(playing, TeleprompterStore.speed) {
        while (playing) {
            scroll.scrollTo(scroll.value + (TeleprompterStore.speed * 2f).toInt())
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = TeleprompterStore.opacity))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column {
            // ---- Header: drag handle + controls ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            offsetX += drag.x
                            offsetY += drag.y
                        }
                    }
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = "Drag to move",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )

                CtrlButton(
                    icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    description = if (playing) "Pause" else "Play",
                    active = playing
                ) {
                    playing = !playing
                    TeleprompterStore.persist(context)
                }

                CtrlButton(Icons.Filled.Remove, "Slower") {
                    TeleprompterStore.speed = (TeleprompterStore.speed - 0.5f).coerceAtLeast(0.5f)
                    TeleprompterStore.persist(context)
                }
                Text(
                    "%.1f".format(TeleprompterStore.speed),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    modifier = Modifier.width(24.dp)
                )
                CtrlButton(Icons.Filled.Add, "Faster") {
                    TeleprompterStore.speed = (TeleprompterStore.speed + 0.5f).coerceAtMost(12f)
                    TeleprompterStore.persist(context)
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    "A-",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            TeleprompterStore.fontSize =
                                (TeleprompterStore.fontSize - 3f).coerceAtLeast(14f)
                            TeleprompterStore.persist(context)
                        }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                )
                Text(
                    "A+",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            TeleprompterStore.fontSize =
                                (TeleprompterStore.fontSize + 3f).coerceAtMost(64f)
                            TeleprompterStore.persist(context)
                        }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                )

                CtrlButton(
                    icon = Icons.Filled.Flip,
                    description = "Mirror",
                    active = TeleprompterStore.mirror
                ) {
                    TeleprompterStore.mirror = !TeleprompterStore.mirror
                    TeleprompterStore.persist(context)
                }

                Text(
                    "${(TeleprompterStore.opacity * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            TeleprompterStore.cycleOpacity()
                            TeleprompterStore.persist(context)
                        }
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                )

                Spacer(Modifier.weight(1f))

                CtrlButton(Icons.Filled.Edit, "Edit script") { onEditScript() }
                CtrlButton(Icons.Filled.Close, "Close") {
                    playing = false
                    onClose()
                }
            }

            // ---- Scrolling script ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text(
                    text = TeleprompterStore.script,
                    color = Color.White,
                    fontSize = TeleprompterStore.fontSize.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = (TeleprompterStore.fontSize * 1.35f).sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = if (TeleprompterStore.mirror) -1f else 1f }
                        .verticalScroll(scroll)
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CtrlButton(
    icon: ImageVector,
    description: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Accent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}
