package com.auradock.dock

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLineTest {
    private val hour = 60 * 60_000L

    @Test fun `a new session fills nothing`() = assertEquals(0f, sessionLineFill(0, hour), 0f)
    @Test fun `half an hour fills half`() = assertEquals(0.5f, sessionLineFill(hour / 2, hour), 0.0001f)
    @Test fun `a full hour starts the next lap`() = assertEquals(0f, sessionLineFill(hour, hour), 0f)
    @Test fun `ninety minutes is half of the second lap`() = assertEquals(0.5f, sessionLineFill(hour + hour / 2, hour), 0.0001f)
    @Test fun `a time before the start fills nothing`() = assertEquals(0f, sessionLineFill(-5_000, hour), 0f)
}
