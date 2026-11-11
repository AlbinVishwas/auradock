package com.auradock.analytics

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {
    private val original = FeatureFlags.reader

    @After fun restore() { FeatureFlags.reader = original }

    @Test fun `a flag that cannot be read is on`() {
        FeatureFlags.reader = { null }
        FeatureFlags.ALL.forEach { assertTrue("$it should default on", FeatureFlags.isOn(it)) }
    }

    @Test fun `a reader that throws leaves the flag on`() {
        FeatureFlags.reader = { error("offline") }
        assertTrue(FeatureFlags.isOn(FeatureFlags.WORLD_TEMPLATE))
    }

    @Test fun `false, the string false and off turn a flag off`() {
        FeatureFlags.reader = { false }
        assertFalse(FeatureFlags.isOn(FeatureFlags.NOW_PLAYING))
        assertFalse(FeatureFlags.interpret("false"))
        assertFalse(FeatureFlags.interpret("OFF"))
    }

    @Test fun `true and any variant name leave it on`() {
        assertTrue(FeatureFlags.interpret(true))
        assertTrue(FeatureFlags.interpret("test-variant"))
    }

    @Test fun `only the flag asked about is read`() {
        FeatureFlags.reader = { key -> if (key == FeatureFlags.DOCK_FROM_ANYWHERE) false else null }
        assertFalse(FeatureFlags.isOn(FeatureFlags.DOCK_FROM_ANYWHERE))
        assertTrue(FeatureFlags.isOn(FeatureFlags.NOW_PLAYING))
    }
}
