package com.auradock.robolectric

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the Robolectric setup works: a real Android Context, the app's package name and its resources, with no emulator. It uses a plain
 * [Application] rather than the app's own, so Hilt and the monitoring and analytics start-up do not run.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class RobolectricSmokeTest {
    @Test fun `the app context loads with its package and resources`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertNotNull(context)
        assertEquals("com.auradock", context.packageName)
        assertEquals("AuraDock", context.getString(context.applicationInfo.labelRes.takeIf { it != 0 } ?: com.auradock.R.string.app_name))
    }
}
