package com.crozzers.postboxgo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.Thread.sleep
import kotlin.properties.Delegates


private const val PACKAGE = "com.crozzers.postboxgo"

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {
    @get:Rule
    var permissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    private lateinit var device: UiDevice
    private var run = false
    private var prefix = "phone"

    @Before
    fun startApp() {
        // check that screenshots are meant to be running
        run = InstrumentationRegistry.getArguments().getString("screenshots") == "1"
        if (!run) {
            return
        }

        if (InstrumentationRegistry.getArguments().getString("device") == "tablet") {
            prefix = "tablet"
        }

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        val launcherPackage: String = device.launcherPackageName
        assertThat(launcherPackage, notNullValue())
        device.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            5000L
        )
        // Launch the app
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(
            PACKAGE
        ).apply {
            // Clear out any previous instances
            this!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        // Wait for the app to appear
        device.wait(
            Until.hasObject(By.pkg(PACKAGE).depth(0)),
            5000L
        )

        // import save file
        val deviceDir = if (prefix == "phone") "sdk_gphone" else "Pixel Tablet"
        waitAndClick(By.text("Settings"))
        waitAndClick(By.text("Import and overwrite"))
        if (device.findObject(By.text("base.json")) != null) {
            device.findObject(By.text("base.json")).click()
        } else {
            waitAndClick(By.desc("Show roots"))
            waitAndClick(By.textContains(deviceDir))
            // sometimes the picker will already be on that path and it selects the breadcrumb widget
            // instead so we have to try again
            if (device.findObject(By.text("Recent")) != null) {
                waitAndClick(By.desc("Show roots"))
                waitAndClick(By.textContains(deviceDir))
                waitAndClick(By.textContains("Download"))
            }
            waitAndClick(By.textContains("Download"))
            waitAndClick(By.text("base.json"))
        }
        waitAndClick(By.text("List View"))
        device.waitForIdle(2000L)
        sleep(1000)  // wait for "imported savefile" msg to go away
    }

    @Test
    fun homepage() {
        Assume.assumeTrue(run)

        val title = device.findObject(By.text("PostboxGO"))
        assertThat(title, notNullValue())

        assert(screenshot(device, "${prefix}_homepage.png"))
    }

    @Test
    fun detailsView() {
        Assume.assumeTrue(run)

        waitAndClick(By.textStartsWith("Grays Inn"))
        device.wait(Until.hasObject(By.textStartsWith("ID: ")), 2000L)
        device.waitForIdle(2000L)  // let map load
        sleep(500L)

        assert(screenshot(device, "${prefix}_details_view.png"))
    }

    @Test
    fun mapView() {
        Assume.assumeTrue(run)

        waitAndClick(By.text("Map View"))
        sleep(3000)
        assert(screenshot(device, "${prefix}_map_view.png"))
    }

    @Test
    fun addPostboxView() {
        Assume.assumeTrue(run)

        waitAndClick(By.text("Register"))
        device.wait(Until.hasObject(By.text("Save Postbox")), 2000L)
        waitAndClick(By.text("Select Postbox"))
        waitAndClick(By.textContains("miles away"), 10000L)
        device.waitForIdle(5000L)
        waitAndClick(By.text("Unmarked"))
        device.waitForIdle(2000L)
        waitAndClick(By.textContains("George 6th"))
        device.waitForIdle(2000L)  // let map load
        assert(screenshot(device, "${prefix}_add_postbox_view.png"))
    }

    @Test
    fun darkMode() {
        Assume.assumeTrue(run)

        waitAndClick(By.text("Settings"))
        sleep(2000)
        waitAndClick(By.text("Standard"))
        waitAndClick(By.text("Dark"))
        sleep(500)
        waitAndClick(By.text("List View"))
        sleep(1000)
        assert(screenshot(device, "${prefix}_dark_theme.png"))
    }

    private fun waitAndClick(selector: BySelector, timeout: Long = 500L) {
        device.wait(Until.hasObject(selector), timeout)
        device.findObject(selector).click()
        device.waitForIdle(500L)
    }
}

fun screenshot(device: UiDevice, name: String): Boolean {
    return device.takeScreenshot(File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "pbg/$name"
    ))
}