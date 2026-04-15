package com.example.whiteboardcaptureandroid

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private fun launchTestActivity(): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DISABLE_CAMERA_START, true)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun activity_launches() {
        launchTestActivity().use {
            onView(withId(R.id.previewView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun statusText_showsInitialMessage() {
        launchTestActivity().use {
            onView(withId(R.id.statusText))
                .check(matches(withText("Auto scan ready")))
        }
    }

    @Test
    fun captureButton_isDisplayed() {
        launchTestActivity().use {
            onView(withId(R.id.captureButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun flashButton_isDisplayed() {
        launchTestActivity().use {
            onView(withId(R.id.flashButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun galleryButton_isDisplayed() {
        launchTestActivity().use {
            onView(withId(R.id.galleryButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun overlayView_isDisplayed() {
        launchTestActivity().use {
            onView(withId(R.id.overlayView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun modeRecyclerView_isDisplayed() {
        launchTestActivity().use {
            onView(withId(R.id.modeRecyclerView)).check(matches(isDisplayed()))
        }
    }
}