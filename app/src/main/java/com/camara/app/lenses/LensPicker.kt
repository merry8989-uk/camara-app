package com.camara.app.lenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.ui.theme.Accent

/** Horizontal lens picker. Each chip shows the lens name over a tinted swatch. */
@Composable
fun LensPicker(
    selected: Lens?,
    onSelect: (Lens) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(LensCatalog.ALL, key = { it.id }) { lens ->
            val isSelected = lens.id == (selected?.id ?: LensCatalog.NONE.id)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(lens) }
                    .padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Accent else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lensSwatch(lens),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = lens.name,
                    color = if (isSelected) Accent else Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

/** A short glyph for the lens chip — first emoji, or a symbol for drawn lenses. */
private fun lensSwatch(lens: Lens): String {
    lens.art.filterIsInstance<EmojiArt>().firstOrNull()?.let { return it.emoji }
    return when (lens.art.filterIsInstance<DrawnArt>().firstOrNull()?.kind) {
        DrawKind.GLASSES -> "🕶"
        DrawKind.CAT_EARS -> "🐱"
        DrawKind.MUSTACHE -> "👨"
        DrawKind.BLUSH -> "☺"
        DrawKind.CROWN -> "👑"
        null -> "◯"
    }
}

/** Small banner shown when no face is detected but a lens is active. */
@Composable
fun NoFaceHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x99000000))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("Point the camera at a face", color = Color.White, fontSize = 12.sp)
    }
}
