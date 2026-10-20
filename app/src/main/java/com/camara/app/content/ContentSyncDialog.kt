package com.camara.app.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camara.app.ui.theme.Accent
import kotlinx.coroutines.launch

/**
 * Content sync sheet: shows where the catalogue comes from, what has been pulled
 * down, and lets the user sync on demand. The app works fine without ever syncing —
 * this only *adds* remote content on top of the built-ins.
 */
@Composable
fun ContentSyncDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(ContentStore.sourceUrl) }

    LaunchedEffect(Unit) { ContentStore.loadCache(context) }

    val syncing = ContentStore.status == SyncStatus.SYNCING

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Content") },
        text = {
            Column {
                Text(
                    "Filters, sticker packs and AR lenses can be updated from a catalogue " +
                        "without reinstalling the app. Built-in content always works offline.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Filters", ContentStore.remoteFilterCount)
                    StatChip("Packs", ContentStore.remotePackCount)
                    StatChip("Lenses", ContentStore.remoteLensCount)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Last sync: ${ContentStore.lastSyncLabel ?: "never"}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                ContentStore.message?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = Accent, fontSize = 11.sp)
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        ContentStore.sourceUrl = it
                    },
                    label = { Text("Catalogue URL", fontSize = 11.sp) },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    "Tip: keep the catalogue in your own repo and edit it directly — push a " +
                        "change and every install picks it up on the next sync.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Text(
                text = if (syncing) "Syncing…" else "Sync now",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (syncing) Color.White.copy(alpha = 0.15f) else Accent)
                    .clickable(enabled = !syncing) {
                        scope.launch { ContentStore.sync(context) }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun StatChip(label: String, value: Int) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text("$value", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}
