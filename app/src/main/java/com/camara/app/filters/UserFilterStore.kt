package com.camara.app.filters

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Filters the user saved themselves.
 *
 * A saved filter is just a colour matrix, built by composing whatever was on screen when
 * they hit save — so it behaves exactly like a built-in one everywhere: the strength
 * slider, the live preview, photos and video all read the same 4x5 matrix.
 *
 * The list is Compose state, so the filter strip picks up a new filter immediately
 * without anything having to reload.
 */
object UserFilterStore {

    private const val FILE = "user_filters.json"
    private const val MATRIX_SIZE = 20

    private val filters = mutableStateListOf<Filter>()

    /** Saved filters, newest last. */
    val all: List<Filter> get() = filters.toList()

    fun isEmpty(): Boolean = filters.isEmpty()

    fun isUserFilter(id: String): Boolean = filters.any { it.id == id }

    /** Call once at startup. A corrupt file is ignored rather than crashing the app. */
    fun load(context: Context) {
        val parsed = runCatching {
            val file = File(context.filesDir, FILE)
            if (!file.exists()) return@runCatching emptyList()
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { fromJson(array.optJSONObject(it)) }
        }.getOrDefault(emptyList())

        filters.clear()
        filters.addAll(parsed)
    }

    /** Saves [filter] under [name]. Returns the stored filter. */
    fun add(context: Context, name: String, matrix: FloatArray): Filter {
        val filter = Filter(
            id = "user_${System.currentTimeMillis()}",
            name = name.trim().ifBlank { "My filter" },
            category = FilterCategory.MINE,
            // Always stored as exactly 20 floats, whatever was handed in.
            matrix = FloatArray(MATRIX_SIZE) { i -> matrix.getOrElse(i) { 0f } }
        )
        filters.add(filter)
        persist(context)
        return filter
    }

    fun remove(context: Context, id: String) {
        filters.removeAll { it.id == id }
        persist(context)
    }

    fun rename(context: Context, id: String, name: String) {
        val index = filters.indexOfFirst { it.id == id }
        if (index < 0) return
        filters[index] = filters[index].copy(name = name.trim().ifBlank { filters[index].name })
        persist(context)
    }

    private fun persist(context: Context) {
        runCatching {
            val array = JSONArray()
            filters.forEach { array.put(toJson(it)) }
            File(context.filesDir, FILE).writeText(array.toString())
        }
    }

    // Kept separate and pure so the format can be round-tripped in a test.

    internal fun toJson(filter: Filter): JSONObject = JSONObject().apply {
        put("id", filter.id)
        put("name", filter.name)
        put("matrix", JSONArray(filter.matrix.toList()))
    }

    internal fun fromJson(json: JSONObject?): Filter? {
        if (json == null) return null
        val array = json.optJSONArray("matrix") ?: return null
        // A matrix that isn't 20 floats is not a filter we can use; drop it rather than
        // letting a bad entry break every filter in the strip.
        if (array.length() != MATRIX_SIZE) return null
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val matrix = FloatArray(MATRIX_SIZE) { array.optDouble(it, 0.0).toFloat() }
        return Filter(
            id = id,
            name = json.optString("name").ifBlank { "My filter" },
            category = FilterCategory.MINE,
            matrix = matrix
        )
    }
}
