package com.camara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.beauty.BeautySettings
import com.camara.app.ui.theme.Accent

/** Vertical panel of beauty sliders. Each row drives one [BeautySettings] field. */
@Composable
fun BeautyPanel(
    settings: BeautySettings,
    onChange: (BeautySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xF214141A))
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .height(230.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Beauty",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BeautySlider("Smooth", settings.smooth, 0f, 1f) { onChange(settings.copy(smooth = it)) }
        BeautySlider("Brighten", settings.brighten, 0f, 1f) { onChange(settings.copy(brighten = it)) }
        BeautySlider("Warmth", settings.warmth, -1f, 1f) { onChange(settings.copy(warmth = it)) }
        BeautySlider("Skin tone", settings.skinTone, -1f, 1f) { onChange(settings.copy(skinTone = it)) }
        BeautySlider("Slim face", settings.slimFace, 0f, 1f) { onChange(settings.copy(slimFace = it)) }
        BeautySlider("Enlarge eyes", settings.enlargeEyes, 0f, 1f) { onChange(settings.copy(enlargeEyes = it)) }
        BeautySlider("Sharpen", settings.sharpen, 0f, 1f) { onChange(settings.copy(sharpen = it)) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BeautySlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier.width(92.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Text(
            "%.2f".format(value),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.width(38.dp)
        )
    }
}
