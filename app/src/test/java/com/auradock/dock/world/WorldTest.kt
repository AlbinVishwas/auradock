package com.auradock.dock.world

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldTest {
    private fun millis(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 9, h, m, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    @Test fun `the projection puts the frames' cities where the frames put them`() {
        assertEquals(636.0f, WorldProjection.x(73.0), 1.0f)
        assertEquals(187.4f, WorldProjection.y(19.0), 1.0f)
        assertEquals(210.4f, WorldProjection.x(-122.4), 1.0f)
        assertEquals(146.6f, WorldProjection.y(37.8), 1.0f)
        assertEquals(476.7f, WorldProjection.x(-0.1), 1.0f)
        assertEquals(116.7f, WorldProjection.y(51.5), 1.0f)
    }

    @Test fun `at 2 13 pm in India it is day over Europe and Asia and night over the Americas`() {
        val sun = Solar.at(millis(8, 43)) // 2:13 pm IST
        assertTrue(Solar.isDay(sun, 51.5, -0.1)) // London, 8:43 am
        assertTrue(Solar.isDay(sun, 19.0, 73.0)) // Navi Mumbai
        assertFalse(Solar.isDay(sun, 37.8, -122.4)) // San Francisco, 12:43 am
        assertFalse(Solar.isDay(sun, 40.7, -74.0)) // New York, 3:43 am
    }

    @Test fun `the day and night line is where the sun is on the horizon`() {
        val sun = Solar.at(millis(8, 43))
        for (lon in listOf(-60.0, 0.0, 45.0, 120.0)) {
            val lat = Solar.terminatorLatitude(sun, lon)
            assertEquals(0.0, Solar.sineAltitude(sun, lat, lon), 1e-6)
        }
        // November: the line peaks near 73 degrees north under the sun's meridian.
        assertEquals(73.0, Solar.terminatorLatitude(sun, Math.toDegrees(sun.subsolarLongitude)), 1.5)
    }

    @Test fun `the sun is over the longitude where it is noon`() {
        val sun = Solar.at(millis(12, 0))
        assertEquals(0.0, Math.toDegrees(sun.subsolarLongitude), 6.0) // within the equation of time (about 16 minutes in November)
    }

    @Test fun `the land mask is 56 by 20 and has land in the right places`() {
        assertEquals(20, LandMask.rows.size)
        LandMask.rows.forEach { assertEquals(56, it.length) }
        // Navi Mumbai's neighbourhood: India's west coast is land on the map.
        val i = Math.round((WorldProjection.x(73.0) - WorldProjection.GRID_X0) / WorldProjection.GRID)
        val j = Math.round((WorldProjection.y(19.0) - WorldProjection.GRID_Y0) / WorldProjection.GRID)
        assertTrue(LandMask.isLand(i, j))
    }

    @Test fun `labels never overlap each other or the home label`() {
        val bounds = Box(0f, 0f, 915f, 412f)
        val home = Box(40f, 200f, 380f, 388f)
        // Two cities close together (as London and Dublin would be) must still get separate labels.
        val placed = placeLabels(
            listOf(LabelRequest("a", 476f, 117f, 90f, 22f), LabelRequest("b", 470f, 119f, 90f, 22f), LabelRequest("c", 210f, 147f, 70f, 22f)),
            listOf(home), bounds
        )
        for (a in placed) for (b in placed) if (a !== b) assertFalse("${a.id} overlaps ${b.id}", a.box.overlaps(b.box))
        placed.forEach { assertFalse(it.box.overlaps(home)) }
        placed.forEach { assertTrue(it.box.left >= 0f && it.box.right <= 915f) }
        // The first one goes to the right of its dot, as in the frame.
        assertTrue(placed[0].box.left > 476f)
    }

    @Test fun `search finds cities by name label or zone`() {
        assertTrue(WorldCities.search("lon").any { it.name == "London" })
        assertTrue(WorldCities.search("sf").any { it.name == "San Francisco" })
        assertTrue(WorldCities.search("kolkata").isNotEmpty())
        assertTrue(WorldCities.search("").size > 50)
    }

    @Test fun `a zone without a bundled city gets a name and a longitude from its offset`() {
        val fallback = WorldCities.forZone(ZoneId.of("Asia/Thimphu"), millis(8, 0))
        assertEquals("Thimphu", fallback.name)
        assertEquals(90.0, fallback.point.lon, 0.01)
    }
}
