package com.camara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.filters.Filter
import com.camara.app.filters.FilterPresets
import com.camara.app.filters.FilterSwatch
import com.camara.app.ui.theme.Accent

/**
 * Horizontal carousel of filters with live colour swatches. Used by both the
 * camera and the editors.
 */
@Composable
fun FilterStrip(
    filters: List<Filter>,
    selected: Filter,
    onSelect: (Filter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
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
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .padding(horizontal = 2.dp)
                )
            }
        }
    }
}

/** Convenience overload that renders the whole built-in library. */
@Composable
fun FilterStrip(
    selected: Filter,
    onSelect: (Filter) -> Unit,
    modifier: Modifier = Modifier
) = FilterStrip(FilterPresets.ALL, selected, onSelect, modifier)
