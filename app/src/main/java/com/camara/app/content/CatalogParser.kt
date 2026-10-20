package com.camara.app.content

import com.camara.app.filters.Filter
import com.camara.app.filters.FilterCategory
import com.camara.app.lenses.Anchor
import com.camara.app.lenses.DrawKind
import com.camara.app.lenses.DrawnArt
import com.camara.app.lenses.EmojiArt
import com.camara.app.lenses.Lens
import com.camara.app.lenses.LensArt
import com.camara.app.stickers.StickerPack
import org.json.JSONArray
import org.json.JSONObject

/**
 * Remote content: filters, sticker packs and AR lenses served from a JSON catalogue
 * so new content can be added without shipping a new APK.
 *
 * Note on size: filters are 20 floats of data, so they cost essentially nothing in
 * the APK either way. The catalogue is about *updating content without a release*,
 * not about shrinking the download. (The APK's size comes from ML models and code,
 * which is handled separately by using unbundled ML Kit and R8.)
 */
object CatalogParser {

    fun parse(json: String): RemoteCatalog {
        val root = JSONObject(json)
        return RemoteCatalog(
            version = root.optInt("version", 0),
            updated = root.optString("updated", ""),
            filters = parseFilters(root.optJSONArray("filters")),
            stickerPacks = parsePacks(root.optJSONArray("stickerPacks")),
            lenses = parseLenses(root.optJSONArray("lenses"))
        )
    }

    private fun parseFilters(arr: JSONArray?): List<Filter> {
        if (arr == null) return emptyList()
        val out = ArrayList<Filter>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").ifBlank { continue }
            val name = o.optString("name", id)
            val category = runCatching {
                FilterCategory.valueOf(o.optString("category", "CLASSIC").uppercase())
            }.getOrDefault(FilterCategory.CLASSIC)
            val matrixArr = o.optJSONArray("matrix") ?: continue
            if (matrixArr.length() != 20) continue
            val matrix = FloatArray(20) { matrixArr.optDouble(it, 0.0).toFloat() }
            out += Filter(id, name, category, matrix)
        }
        return out
    }

    private fun parsePacks(arr: JSONArray?): List<StickerPack> {
        if (arr == null) return emptyList()
        val out = ArrayList<StickerPack>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").ifBlank { continue }
            val s = o.optJSONArray("stickers") ?: continue
            val stickers = ArrayList<String>(s.length())
            for (j in 0 until s.length()) {
                val v = s.optString(j)
                if (v.isNotBlank()) stickers += v
            }
            if (stickers.isNotEmpty()) out += StickerPack(name, stickers)
        }
        return out
    }

    private fun parseLenses(arr: JSONArray?): List<Lens> {
        if (arr == null) return emptyList()
        val out = ArrayList<Lens>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id").ifBlank { continue }
            val name = o.optString("name", id)
            val artArr = o.optJSONArray("art") ?: continue
            val art = ArrayList<LensArt>(artArr.length())
            for (j in 0 until artArr.length()) {
                val a = artArr.optJSONObject(j) ?: continue
                val anchor = runCatching {
                    Anchor.valueOf(a.optString("anchor", "EYE_MID").uppercase())
                }.getOrDefault(Anchor.EYE_MID)
                val dx = a.optDouble("dx", 0.0).toFloat()
                val dy = a.optDouble("dy", 0.0).toFloat()
                val size = a.optDouble("size", 1.0).toFloat()
                when (a.optString("type", "emoji")) {
                    "drawn" -> {
                        val kind = runCatching {
                            DrawKind.valueOf(a.optString("kind", "GLASSES").uppercase())
                        }.getOrNull() ?: continue
                        art += DrawnArt(kind, anchor, dx, dy, size)
                    }
                    else -> {
                        val emoji = a.optString("emoji")
                        if (emoji.isNotBlank()) art += EmojiArt(emoji, anchor, dx, dy, size)
                    }
                }
            }
            if (art.isNotEmpty()) out += Lens(id, name, art)
        }
        return out
    }
}

data class RemoteCatalog(
    val version: Int,
    val updated: String,
    val filters: List<Filter>,
    val stickerPacks: List<StickerPack>,
    val lenses: List<Lens>
) {
    val isEmpty: Boolean
        get() = filters.isEmpty() && stickerPacks.isEmpty() && lenses.isEmpty()

    companion object {
        val EMPTY = RemoteCatalog(0, "", emptyList(), emptyList(), emptyList())
    }
}
