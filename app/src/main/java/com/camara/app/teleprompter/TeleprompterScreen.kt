package com.camara.app.teleprompter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.ui.theme.Accent
import kotlinx.coroutines.delay

/**
 * A teleprompter: paste a script, set speed and text size, then hit play and the
 * text scrolls for you to read while recording. Optional mirror mode flips the
 * text for beam-splitter glass rigs.
 */
@Composable
fun TeleprompterScreen(onBack: () -> Unit) {
    var script by remember {
        mutableStateOf(
            "Paste your script here.\n\n" +
                "Hit play and Camara scrolls the words at your pace. " +
                "Adjust speed and text size below, or flip the text with mirror mode " +
                "when you are using a teleprompter rig."
        )
    }
    var speed by remember { mutableFloatStateOf(2.5f) }   // scroll units per tick
    var fontSize by remember { mutableFloatStateOf(34f) }
    var mirror by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(true) }

    val scroll = rememberScrollState()

    LaunchedEffect(playing, speed) {
        while (playing) {
            scroll.scrollTo(scroll.value + (speed * 2f).toInt())
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Teleprompter",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { editing = !editing }) {
                    Text(
                        if (editing) "Done" else "Edit",
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Script area
            if (editing) {
                OutlinedTextField(
                    value = script,
                    onValueChange = { script = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = script,
                        color = Color.White,
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = (fontSize * 1.4f).sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { scaleX = if (mirror) -1f else 1f }
                            .verticalScroll(scroll)
                    )
                }
            }

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF14141A))
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                    Slider(
                        value = speed,
                        onValueChange = { speed = it },
                        valueRange = 0.5f..8f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Text size", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 18f..72f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Flip, contentDescription = "Mirror", tint = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(0.dp))
                        Text("Mirror", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        Switch(checked = mirror, onCheckedChange = { mirror = it })
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Accent)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .clickable { playing = !playing; editing = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White
                            )
                            Text(
                                if (playing) "Pause" else "Play",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
