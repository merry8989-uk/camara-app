package com.camara.app.editor.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.ui.theme.Accent
import com.camara.app.video.VideoOverlay
import kotlin.math.roundToInt

private val TEXT_COLORS = listOf(
    0xFFFFFFFFL, 0xFF000000L, 0xFFFF2D78L, 0xFFFFD400L, 0xFF29D9A6L, 0xFF7C4DFFL,
)

/** A small, fixed set so the panel stays compact; the full catalogue lives in the camera. */
private val STICKER_GLYPHS = listOf(
    "😀", "😎", "❤️", "🔥", "✨", "🎉", "⭐", "🌸", "😂", "👍",
)

/**
 * Text and sticker overlays for the video.
 *
 * An overlay is placed by tapping it on the preview and dragging, so this panel only has
 * to cover adding, restyling and timing. Sizes and positions are fractions of the frame,
 * which is why the preview and the export agree.
 */
@Composable
fun VideoOverlayPanel(
    overlays: List<VideoOverlay>,
    selectedId: Long?,
    clipMs: Float,
    playheadMs: Float,
    onAddText: () -> Unit,
    onAddSticker: (String) -> Unit,
    onSelect: (Long?) -> Unit,
    onChange: (VideoOverlay) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val selected = overlays.firstOrNull { it.id == selectedId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC101014))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // ---- What is on the video, plus the two ways to add ----
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Accent.copy(alpha = 0.9f))
                        .clickable { onAddText() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.TextFields, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Text(
                        "Add text",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }

            items(overlays, key = { it.id }) { overlay ->
                val isSelected = overlay.id == selectedId
                val label = when (overlay.kind) {
                    VideoOverlay.Kind.TEXT -> overlay.text.ifBlank { "Text" }
                    VideoOverlay.Kind.STICKER -> overlay.glyph
                }
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) Accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) Accent else Color.Transparent,
                            shape = RoundedCornerShape(18.dp),
                        )
                        .clickable { onSelect(overlay.id) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // ---- Stickers ----
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(STICKER_GLYPHS) { glyph ->
                Text(
                    text = glyph,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onAddSticker(glyph) }
                        .padding(6.dp)
                )
            }
        }

        if (selected == null) {
            Text(
                "Add text or a sticker, then tap it on the preview and drag to place it.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }

        // ---- The selected overlay ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (selected.kind == VideoOverlay.Kind.TEXT) "Size" else "Size",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier.width(46.dp)
            )
            Slider(
                value = selected.sizeFraction,
                onValueChange = { onChange(selected.copy(sizeFraction = it)) },
                valueRange = 0.03f..0.30f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete overlay",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onDelete(selected.id) }
                    .padding(6.dp)
                    .size(18.dp)
            )
        }

        if (selected.kind == VideoOverlay.Kind.TEXT) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TEXT_COLORS.forEach { argb ->
                    val isChosen = selected.color == argb
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (isChosen) 2.dp else 1.dp,
                                color = if (isChosen) Accent else Color.White.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable { onChange(selected.copy(color = argb)) }
                    )
                }
            }
        }

        // ---- When it shows ----
        val clip = clipMs.coerceAtLeast(1f)
        TimeSlider(
            label = "From",
            value = selected.startMs.coerceIn(0f, clip),
            clipMs = clip,
            onChange = { start ->
                val end = selected.endMs.coerceAtLeast(start)
                onChange(selected.copy(startMs = start, endMs = end))
            }
        )
        TimeSlider(
            label = "Until",
            value = selected.endMs.coerceIn(0f, clip),
            clipMs = clip,
            onChange = { end ->
                val start = selected.startMs.coerceAtMost(end)
                onChange(selected.copy(startMs = start, endMs = end))
            }
        )
        Text(
            text = if (selected.isFullLength(clip)) {
                "Showing for the whole clip · playhead ${(playheadMs / 1000f).roundToInt()}s"
            } else {
                "Showing ${(selected.startMs / 1000f).roundToInt()}s–" +
                    "${(selected.endMs / 1000f).roundToInt()}s · playhead ${(playheadMs / 1000f).roundToInt()}s"
            },
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun TimeSlider(
    label: String,
    value: Float,
    clipMs: Float,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier.width(46.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..clipMs,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Text(
            "${(value / 1000f).roundToInt()}s",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.width(34.dp)
        )
    }
}
