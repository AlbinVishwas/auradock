package com.auradock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyUrlTest {
    @Test fun `the privacy policy address is the published page, over https`() {
        assertEquals("https://albinvishwas.github.io/auradock/privacy/", PRIVACY_POLICY_URL)
        assertTrue(PRIVACY_POLICY_URL.startsWith("https://"))
    }
}
