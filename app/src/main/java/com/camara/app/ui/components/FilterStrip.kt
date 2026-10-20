package com.camara.app.ui.components

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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.content.ContentStore
import com.camara.app.filters.Filter
import com.camara.app.filters.FilterCategory
import com.camara.app.filters.FilterSwatch
import com.camara.app.ui.theme.Accent
import kotlin.math.roundToInt

/**
 * The filter browser: a category row, a carousel of filters with live colour
 * swatches, and (when [onIntensityChange] is supplied) a strength slider that
 * blends the look toward the original.
 */
@Composable
fun FilterStrip(
    selected: Filter,
    onSelect: (Filter) -> Unit,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    onIntensityChange: ((Float) -> Unit)? = null
) {
    var category by remember(selected) { mutableStateOf(selected.category) }
    // Read through ContentStore so remotely-synced filters appear here too.
    val filters = ContentStore.filtersIn(category)

    Column(modifier = modifier.fillMaxWidth()) {
        // ---- Category tabs ----
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            items(ContentStore.activeCategories(), key = { it.name }) { c ->
                val isSelected = c == category
                Text(
                    text = c.label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Accent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f))
                        .clickable { category = c }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                )
            }
        }

        // ---- Filter carousel ----
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filters, key = { it.id }) { filter ->
                val isSelected = filter.id == selected.id
                val swatch = FilterSwatch.color(filter)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelect(filter) }
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(swatch, swatch.copy(alpha = 0.55f), Color(0xFF101014))
                                )
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Accent else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = filter.name,
                        color = if (isSelected) Accent else Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ---- Strength / blend ----
        if (onIntensityChange != null && !selected.isIdentity) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Strength",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.width(62.dp)
                )
                Slider(
                    value = intensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Accent,
                        activeTrackColor = Accent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Text(
                    "${(intensity * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.width(38.dp)
                )
            }
        }
    }
}
