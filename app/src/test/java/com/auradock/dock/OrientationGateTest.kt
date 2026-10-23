package com.auradock.dock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class OrientationGateTest {
    private val g = 9.81f
    private fun rad(deg: Double) = Math.toRadians(deg)

    // ── classifier ────────────────────────────────────────────────────────────

    @Test
    fun `upright phone is portrait`() =
        assertEquals(PhysicalOrientation.PORTRAIT, classifyOrientation(0f, g, 0f))

    @Test
    fun `top edge to the left is landscape-left`() =
        assertEquals(PhysicalOrientation.LANDSCAPE_LEFT, classifyOrientation(g, 0f, 0f))

    @Test
    fun `top edge to the right is landscape-right`() =
        assertEquals(PhysicalOrientation.LANDSCAPE_RIGHT, classifyOrientation(-g, 0f, 0f))

    @Test
    fun `lying on its back or face down is flat`() {
        assertEquals(PhysicalOrientation.FLAT, classifyOrientation(0f, 0f, g))
        assertEquals(PhysicalOrientation.FLAT, classifyOrientation(0f, 0f, -g))
    }

    @Test
    fun `landscape on a stand leaning back 25 degrees is still landscape`() {
        val tilt = rad(25.0)
        val x = (g * cos(tilt)).toFloat()
        val z = (g * sin(tilt)).toFloat() // screen leans back, so some gravity goes through it
        assertEquals(PhysicalOrientation.LANDSCAPE_LEFT, classifyOrientation(x, 0f, z))
        assertEquals(PhysicalOrientation.LANDSCAPE_RIGHT, classifyOrientation(-x, 0f, z))
    }

    @Test
    fun `portrait on a stand leaning back 25 degrees is still portrait`() {
        val tilt = rad(25.0)
        assertEquals(
            PhysicalOrientation.PORTRAIT,
            classifyOrientation(0f, (g * cos(tilt)).toFloat(), (g * sin(tilt)).toFloat())
        )
    }

    @Test
    fun `stand tilt limit sits between 50 and 55 degrees`() {
        fun landscapeAt(deg: Double) =
            classifyOrientation((g * cos(rad(deg))).toFloat(), 0f, (g * sin(rad(deg))).toFloat())
        assertEquals(PhysicalOrientation.LANDSCAPE_LEFT, landscapeAt(50.0))
        assertEquals(PhysicalOrientation.FLAT, landscapeAt(55.0))
    }

    @Test
    fun `free fall or no gravity is treated as flat`() =
        assertEquals(PhysicalOrientation.FLAT, classifyOrientation(0.2f, 0.1f, 0.3f))

    @Test
    fun `sideways tilt in the screen plane still picks the dominant axis`() {
        assertEquals(PhysicalOrientation.LANDSCAPE_LEFT, classifyOrientation(7f, 5f, 3f))
        assertEquals(PhysicalOrientation.PORTRAIT, classifyOrientation(5f, 7f, 3f))
    }

    // ── content rotation ──────────────────────────────────────────────────────

    @Test
    fun `portrait window, phone turned left or right`() {
        assertEquals(90, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_LEFT, 0))
        assertEquals(270, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_RIGHT, 0))
    }

    @Test
    fun `display already rotated to match needs no extra turn`() {
        assertEquals(0, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_LEFT, 90))
        assertEquals(0, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_RIGHT, 270))
    }

    @Test
    fun `display rotated the opposite way needs a half turn`() {
        assertEquals(180, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_LEFT, 270))
        assertEquals(180, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_RIGHT, 90))
    }

    @Test
    fun `reverse-portrait window`() {
        assertEquals(270, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_LEFT, 180))
        assertEquals(90, contentRotationDegrees(PhysicalOrientation.LANDSCAPE_RIGHT, 180))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `portrait is never rotated for the dock`() {
        contentRotationDegrees(PhysicalOrientation.PORTRAIT, 0)
    }

    // ── stabilizer ────────────────────────────────────────────────────────────

    @Test
    fun `needs the same reading several times in a row`() {
        val s = OrientationStabilizer(requiredConsecutive = 3)
        assertNull(s.onSample(PhysicalOrientation.LANDSCAPE_LEFT))
        assertNull(s.onSample(PhysicalOrientation.LANDSCAPE_LEFT))
        assertEquals(PhysicalOrientation.LANDSCAPE_LEFT, s.onSample(PhysicalOrientation.LANDSCAPE_LEFT))
    }

    @Test
    fun `a different reading resets the count`() {
        val s = OrientationStabilizer(requiredConsecutive = 3)
        s.onSample(PhysicalOrientation.LANDSCAPE_LEFT)
        s.onSample(PhysicalOrientation.LANDSCAPE_LEFT)
        assertNull(s.onSample(PhysicalOrientation.PORTRAIT)) // wobble
        assertNull(s.onSample(PhysicalOrientation.LANDSCAPE_LEFT))
        assertNull(s.onSample(PhysicalOrientation.LANDSCAPE_LEFT))
        assertEquals(PhysicalOrientation.LANDSCAPE_LEFT, s.onSample(PhysicalOrientation.LANDSCAPE_LEFT))
    }

    // ── gate ──────────────────────────────────────────────────────────────────

    @Test
    fun `first stable landscape shows the dock`() {
        val gate = DockOrientationGate()
        assertEquals(
            GateDecision.Show(PhysicalOrientation.LANDSCAPE_RIGHT),
            gate.onStableOrientation(PhysicalOrientation.LANDSCAPE_RIGHT)
        )
    }

    @Test
    fun `first stable portrait or flat finishes`() {
        assertEquals(GateDecision.Finish, DockOrientationGate().onStableOrientation(PhysicalOrientation.PORTRAIT))
        assertEquals(GateDecision.Finish, DockOrientationGate().onStableOrientation(PhysicalOrientation.FLAT))
    }

    @Test
    fun `no stable reading in time finishes`() =
        assertEquals(GateDecision.Finish, DockOrientationGate().onTimeout())

    @Test
    fun `once showing, a bump to portrait is ignored and the timeout does nothing`() {
        val gate = DockOrientationGate()
        gate.onStableOrientation(PhysicalOrientation.LANDSCAPE_LEFT)
        assertEquals(GateDecision.Wait, gate.onStableOrientation(PhysicalOrientation.PORTRAIT))
        assertEquals(GateDecision.Wait, gate.onStableOrientation(PhysicalOrientation.FLAT))
        assertEquals(GateDecision.Wait, gate.onTimeout())
    }

    @Test
    fun `once showing, flipping the stand turns the content`() {
        val gate = DockOrientationGate()
        gate.onStableOrientation(PhysicalOrientation.LANDSCAPE_LEFT)
        assertEquals(
            GateDecision.Show(PhysicalOrientation.LANDSCAPE_RIGHT),
            gate.onStableOrientation(PhysicalOrientation.LANDSCAPE_RIGHT)
        )
    }
}
