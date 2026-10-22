package com.chukwuebuka.qrscanner

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerLaunchTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun launchesIntoReadyScanner() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        assertNotNull("The app must expose a launcher activity", launchIntent)
        context.startActivity(launchIntent!!.apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })

        assertTrue(
            "Scanner title was not visible",
            device.wait(Until.hasObject(By.text("Scan a QR code")), 45_000),
        )
        assertTrue(
            "Scanner guidance was not visible",
            device.hasObject(By.text("Point your camera at a QR code")),
        )
    }
}
