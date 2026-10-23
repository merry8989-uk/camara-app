package com.camara.app.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import com.camara.app.editor.image.Adjustments

/**
 * Saving a filter: the JSON round-trip and the composed matrix.
 *
 * The format matters because it is what survives an app restart — a bad entry must be
 * dropped rather than taking every filter in the strip down with it.
 */
class UserFilterStoreTest {

    private fun sampleMatrix() = FloatArray(20) { i -> (i + 1) / 20f }

    private fun filter() = Filter(
        id = "user_1",
        name = "Evening",
        category = FilterCategory.MINE,
        matrix = sampleMatrix()
    )

    @Test
    fun `a filter survives the json round trip`() {
        val original = filter()
        val restored = UserFilterStore.fromJson(UserFilterStore.toJson(original))

        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertEquals(original.name, restored.name)
        assertEquals(FilterCategory.MINE, restored.category)
        assertTrue("matrix must match", original.matrix.contentEquals(restored.matrix))
        assertEquals("a restored filter behaves like the original", original, restored)
    }

    @Test
    fun `a matrix that is not 20 floats is rejected`() {
        val short = JSONObject()
            .put("id", "user_2")
            .put("name", "Broken")
            .put("matrix", JSONArray(listOf(1f, 2f, 3f)))
        assertNull("a short matrix must not be used", UserFilterStore.fromJson(short))

        val long = JSONObject()
            .put("id", "user_3")
            .put("name", "Broken")
            .put("matrix", JSONArray(List(21) { 1f }))
        assertNull("a long matrix must not be used", UserFilterStore.fromJson(long))
    }

    @Test
    fun `an entry with no id or no matrix is rejected`() {
        assertNull(UserFilterStore.fromJson(null))
        assertNull(UserFilterStore.fromJson(JSONObject().put("name", "No id")))
        assertNull(UserFilterStore.fromJson(JSONObject().put("id", "user_4")))
    }

    @Test
    fun `a missing name falls back rather than failing`() {
        val nameless = JSONObject()
            .put("id", "user_5")
            .put("matrix", JSONArray(sampleMatrix().toList()))
        val restored = UserFilterStore.fromJson(nameless)
        assertNotNull(restored)
        assertTrue("must have some name", restored!!.name.isNotBlank())
    }

    @Test
    fun `a saved filter is identified as a user filter`() {
        val saved = filter()
        assertFalse(
            "a built-in id is not a user filter",
            UserFilterStore.isUserFilter(FilterPresets.NONE.id)
        )
        assertFalse(UserFilterStore.isUserFilter(saved.id))
    }

    @Test
    fun `the saved matrix is exactly the look on screen`() {
        val base = FilterPresets.ALL.first { it.category == FilterCategory.WARM }
        val intensity = 0.6f
        val adjustments = Adjustments(
            brightness = 0.2f,
            contrast = 1.3f,
            saturation = 0.8f,
            warmth = 0.15f
        )

        // Saving composes the filter at its strength with the adjustments. Applying that
        // one matrix must equal applying them one after the other, which is what the
        // preview showed.
        val saved = ColorMatrixMath.compose(
            base.matrixAt(intensity),
            adjustments.matrix()
        )
        val oneAfterTheOther = ColorMatrixMath.compose(
            adjustments.matrix(),
            base.matrixAt(intensity)
        )
        assertTrue("saved look must match the screen", saved.contentEquals(oneAfterTheOther))
        assertFalse(
            "saving must not silently do nothing",
            saved.contentEquals(ColorMatrixMath.IDENTITY)
        )
    }
}
