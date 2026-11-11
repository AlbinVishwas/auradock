package com.auradock.robolectric

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.auradock.focus.FocusSettings
import com.auradock.focus.vip.Vip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The VIP list (names and contact keys) must never sit in a file that Android backs up. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class VipBackupTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun `a list saved by an older version moves out of the backed up file and is still there`() {
        val old = context.getSharedPreferences("auradock_focus_settings", Context.MODE_PRIVATE)
        FocusSettings(context).addVip(Vip("key-1", "Alex"))
        val saved = context.getSharedPreferences("auradock_vips", Context.MODE_PRIVATE).getString("vips", "").orEmpty()
        old.edit().putString("vips", saved).commit() // what 0.3.2 left behind
        context.getSharedPreferences("auradock_vips", Context.MODE_PRIVATE).edit().clear().commit()

        val settings = FocusSettings(context)

        assertEquals(listOf(Vip("key-1", "Alex")), settings.vips.value)
        assertFalse(old.contains("vips"))
    }

    @Test fun `new VIPs are saved only in the file that is not backed up`() {
        FocusSettings(context).addVip(Vip("key-2", "Sam"))

        assertFalse(context.getSharedPreferences("auradock_focus_settings", Context.MODE_PRIVATE).contains("vips"))
        assertTrue(context.getSharedPreferences("auradock_vips", Context.MODE_PRIVATE).contains("vips"))
    }

    @Test fun `the backup rules do not list the VIP file`() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "app/src/main/res/xml").isDirectory }
        File(root, "app/src/main/res/xml").listFiles { f -> f.name.endsWith(".xml") }.orEmpty().forEach {
            assertFalse(it.name, it.readText().contains("path=\"auradock_vips.xml\""))
        }
    }
}
