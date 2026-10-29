package com.camara.app.face

import androidx.compose.ui.geometry.Offset
import com.camara.app.lenses.FaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The face box drives the live effect crop — an error here slides the effect off the face. */
class FaceGeometryTest {

    private fun face(cx: Float, cy: Float, w: Float, h: Float) = FaceState(
        cx = cx, cy = cy, w = w, h = h, imageAspect = 0.75f
    )

    @Test
    fun `the box is centred on the face`() {
        val box = FaceGeometry.faceBox(face(0.5f, 0.5f, 0.4f, 0.5f), 400, 600, expand = 0f)
        assertEquals(160f, box.left, 0.01f)    // (0.5 - 0.2) * 400
        assertEquals(240f, box.right, 0.01f)
        assertEquals(150f, box.top, 0.01f)     // (0.5 - 0.25) * 600
        assertEquals(450f, box.bottom, 0.01f)
    }

    @Test
    fun `expansion grows the box on every side`() {
        val tight = FaceGeometry.faceBox(face(0.5f, 0.5f, 0.4f, 0.5f), 400, 600, expand = 0f)
        val loose = FaceGeometry.faceBox(face(0.5f, 0.5f, 0.4f, 0.5f), 400, 600, expand = 0.25f)
        assertTrue(loose.left < tight.left)
        assertTrue(loose.top < tight.top)
        assertTrue(loose.right > tight.right)
        assertTrue(loose.bottom > tight.bottom)
    }

    @Test
    fun `the box never leaves the frame`() {
        // A face pressed into the top-left corner with a big expansion.
        val box = FaceGeometry.faceBox(face(0.05f, 0.05f, 0.4f, 0.4f), 400, 600, expand = 0.5f)
        assertTrue("left ${box.left}", box.left >= 0f)
        assertTrue("top ${box.top}", box.top >= 0f)
        assertTrue(box.right <= 400f)
        assertTrue(box.bottom <= 600f)
    }

    @Test
    fun `a face off the edge still yields a box inside the frame`() {
        val box = FaceGeometry.faceBox(face(1.2f, -0.1f, 0.3f, 0.3f), 400, 600)
        assertTrue(box.left >= 0f)
        assertTrue(box.top >= 0f)
        assertTrue(box.right <= 400f)
        assertTrue(box.bottom <= 600f)
    }

    @Test
    fun `a degenerate frame gives an empty box`() {
        assertTrue(FaceGeometry.faceBox(face(0.5f, 0.5f, 0.4f, 0.4f), 0, 0).isEmpty)
    }

    @Test
    fun `a tiny face is treated as empty rather than rendered`() {
        // Two pixels is not worth a compositing pass.
        assertTrue(FaceGeometry.faceBox(face(0.5f, 0.5f, 0.001f, 0.001f), 400, 600).isEmpty)
    }

    @Test
    fun `the outline falls back to an ellipse when there is no contour`() {
        val pts = FaceGeometry.outlinePixels(face(0.5f, 0.5f, 0.4f, 0.4f), 400, 600)
        assertTrue("needs enough points for a path", pts.size >= 6)
        // Every point should sit on the ellipse, i.e. inside the box.
        val box = FaceGeometry.faceBox(face(0.5f, 0.5f, 0.4f, 0.4f), 400, 600, expand = 0.05f)
        pts.forEach { (x, y) ->
            assertTrue("x $x outside ${box.left}..${box.right}", x >= box.left && x <= box.right)
            assertTrue("y $y outside ${box.top}..${box.bottom}", y >= box.top && y <= box.bottom)
        }
    }

    @Test
    fun `the outline uses the real contour when one exists`() {
        val contour = listOf(
            Offset(0.3f, 0.2f), Offset(0.7f, 0.2f), Offset(0.7f, 0.8f),
            Offset(0.5f, 0.9f), Offset(0.3f, 0.8f), Offset(0.25f, 0.5f)
        )
        val pts = FaceGeometry.outlinePixels(
            face(0.5f, 0.5f, 0.4f, 0.4f).copy(faceContour = contour), 100, 100
        )
        assertEquals(contour.size, pts.size)
        assertEquals(30f, pts[0].first, 0.01f)
        assertEquals(20f, pts[0].second, 0.01f)
    }

    @Test
    fun `the mask key is stable for sub-pixel movement`() {
        val a = FaceGeometry.maskKey(face(0.5000f, 0.5f, 0.4f, 0.5f), 400, 600)
        val b = FaceGeometry.maskKey(face(0.5001f, 0.5f, 0.4f, 0.5f), 400, 600)
        assertEquals("must not rebuild the mask for noise", a, b)
    }

    @Test
    fun `the mask key changes when the face really moves`() {
        val a = FaceGeometry.maskKey(face(0.50f, 0.5f, 0.4f, 0.5f), 400, 600)
        val b = FaceGeometry.maskKey(face(0.56f, 0.5f, 0.4f, 0.5f), 400, 600)
        assertTrue("a real move must rebuild the mask", a != b)
    }
}
