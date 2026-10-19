package com.camara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.stickers.StickerPacks
import com.camara.app.ui.theme.Accent

/** Tabbed sticker picker. Tapping a sticker emits its glyph to be placed. */
@Composable
fun StickerPanel(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableIntStateOf(0) }
    val pack = StickerPacks.PACKS[tab]

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xF214141A))
            .padding(vertical = 10.dp)
            .height(240.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StickerPacks.PACKS.forEachIndexed { index, p ->
                val selected = index == tab
                Text(
                    p.name,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f))
                        .clickable { tab = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(pack.stickers) { glyph ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { onPick(glyph) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(glyph, fontSize = 24.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
