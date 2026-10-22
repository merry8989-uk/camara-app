package com.camara.app.content

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.camara.app.filters.Filter
import com.camara.app.filters.FilterCategory
import com.camara.app.filters.FilterPresets
import com.camara.app.lenses.Lens
import com.camara.app.lenses.LensCatalog
import com.camara.app.stickers.StickerPack
import com.camara.app.stickers.StickerPacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SyncStatus { IDLE, SYNCING, OK, FAILED }

/**
 * Holds the merged content set — built-in defaults plus anything fetched from the
 * remote catalogue — and exposes it to the UI.
 *
 * Remote entries are merged by id: a remote filter/lens with an existing id
 * replaces the built-in one, a new id is appended. Everything falls back to the
 * built-ins, so the app works fully offline.
 */
object ContentStore {

    /** Edit this to point at your own catalogue. */
    const val DEFAULT_URL =
        "https://raw.githubusercontent.com/merry8989-uk/camara-app/main/content/catalog.json"

    private const val PREFS = "camara_content"
    private const val CACHE_FILE = "catalog.json"

    var status by mutableStateOf(SyncStatus.IDLE); private set
    var message by mutableStateOf<String?>(null); private set
    var lastSyncLabel by mutableStateOf<String?>(null); private set
    var sourceUrl by mutableStateOf(DEFAULT_URL)
    var remoteFilterCount by mutableStateOf(0); private set
    var remotePackCount by mutableStateOf(0); private set
    var remoteLensCount by mutableStateOf(0); private set

    private var remote: RemoteCatalog = RemoteCatalog.EMPTY

    // ------------------------------------------------------------------ merged

    val filters: List<Filter>
        get() = mergeById(FilterPresets.ALL, remote.filters) { it.id }

    val packs: List<StickerPack>
        get() = mergeByKey(StickerPacks.PACKS, remote.stickerPacks) { it.name }

    val lenses: List<Lens>
        get() = mergeById(LensCatalog.ALL, remote.lenses) { it.id }

    fun filtersIn(category: FilterCategory): List<Filter> =
        if (category == FilterCategory.ORIGINAL) listOf(FilterPresets.NONE)
        else filters.filter { it.category == category }

    fun activeCategories(): List<FilterCategory> =
        FilterCategory.entries.filter { filtersIn(it).isNotEmpty() }

    fun lensById(id: String): Lens = lenses.firstOrNull { it.id == id } ?: LensCatalog.NONE

    private fun <T> mergeById(builtIn: List<T>, extra: List<T>, id: (T) -> String): List<T> {
        if (extra.isEmpty()) return builtIn
        val out = builtIn.toMutableList()
        extra.forEach { e ->
            val idx = out.indexOfFirst { id(it) == id(e) }
            if (idx >= 0) out[idx] = e else out.add(e)
        }
        return out
    }

    private fun <T> mergeByKey(builtIn: List<T>, extra: List<T>, key: (T) -> String): List<T> =
        mergeById(builtIn, extra, key)

    // -------------------------------------------------------------------- sync

    /** Loads the cached catalogue (if any) without touching the network. */
    fun loadCache(context: Context) {
        try {
            val f = File(context.filesDir, CACHE_FILE)
            if (f.exists()) {
                remote = CatalogParser.parse(f.readText())
                applyCounts()
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                lastSyncLabel = prefs.getString("lastSync", null)
                sourceUrl = prefs.getString("url", DEFAULT_URL) ?: DEFAULT_URL
                status = SyncStatus.OK
            }
        } catch (t: Throwable) {
            // A bad cache must never break the app: fall back to built-ins.
            remote = RemoteCatalog.EMPTY
            status = SyncStatus.IDLE
        }
    }

    /**
     * Fetches the catalogue, caches it, and merges. Falls back to the cache.
     *
     * Compose snapshot state is only ever written on the main thread — writing it from
     * a background dispatcher is not safe. The network and disk work is dispatched off
     * the main thread and the results are published back on it.
     */
    suspend fun sync(context: Context): Boolean {
        withContext(Dispatchers.Main.immediate) {
            status = SyncStatus.SYNCING
            message = null
        }
        return try {
            val url = sourceUrl
            val text = withContext(Dispatchers.IO) { httpGet(url) }
            val parsed = withContext(Dispatchers.Default) { CatalogParser.parse(text) }
            val label = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

            withContext(Dispatchers.IO) {
                File(context.filesDir, CACHE_FILE).writeText(text)
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("lastSync", label)
                    .putString("url", url)
                    .apply()
            }

            withContext(Dispatchers.Main.immediate) {
                remote = parsed
                lastSyncLabel = label
                applyCounts()
                status = SyncStatus.OK
                message = if (parsed.isEmpty) {
                    "Catalogue fetched but contained no items."
                } else {
                    "Synced ${parsed.filters.size} filters, ${parsed.stickerPacks.size} sticker packs, ${parsed.lenses.size} lenses."
                }
            }
            true
        } catch (t: Throwable) {
            withContext(Dispatchers.Main.immediate) {
                status = SyncStatus.FAILED
                message = "Sync failed (${t.javaClass.simpleName}). Using built-in content."
            }
            false
        }
    }

    private fun applyCounts() {
        remoteFilterCount = remote.filters.size
        remotePackCount = remote.stickerPacks.size
        remoteLensCount = remote.lenses.size
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
