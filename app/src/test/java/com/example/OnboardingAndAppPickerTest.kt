package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ScreenState
import com.example.ui.VaultViewModel
import com.example.ui.components.AppPickerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingAndAppPickerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("ktp_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `app picker helper provides curated popular indonesian apps`() {
        val popular = AppPickerHelper.POPULAR_APPS
        assertTrue(popular.isNotEmpty())
        assertTrue(popular.any { it.packageName == "com.bca" })
        assertTrue(popular.any { it.packageName == "com.gojek.app" })
        assertTrue(popular.any { it.packageName == "id.bmri.livin" })
    }

    @Test
    fun `app picker helper returns non-empty list of available apps`() {
        val apps = AppPickerHelper.getInstalledApps(context)
        assertNotNull(apps)
        assertTrue(apps.isNotEmpty())
        // Verified distinct package names
        val packageSet = apps.map { it.packageName }.toSet()
        assertEquals(packageSet.size, apps.size)
    }

    @Test
    fun `new user starts on onboarding screen then completes or skips`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = VaultViewModel(app)

        // Initial state before onboarding is complete
        assertEquals(ScreenState.Onboarding, vm.uiState.value.currentScreen)

        // Complete/skip onboarding
        vm.completeOnboarding()

        // Verifies preference persisted
        val prefs = context.getSharedPreferences("ktp_vault_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("onboarding_completed", false))

        // Screen advances
        val current = vm.uiState.value.currentScreen
        assertTrue(current == ScreenState.InitialSetup || current == ScreenState.Dashboard)
    }
}
