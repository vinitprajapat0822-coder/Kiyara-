package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.operating.CapabilityRegistry
import com.example.operating.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext_matchesKiaraa() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KIARAA AI", appName)
  }

  @Test
  fun testCapabilityRiskClassification() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.AppDatabase.getInstance(context)
    val registry = CapabilityRegistry(context, database.auditDao())

    val highRiskCap = registry.capabilities.find { it.riskLevel == RiskLevel.HIGH }
    assertNotNull(highRiskCap)
    assertEquals("system_settings", highRiskCap?.id)

    val lowRiskCaps = registry.capabilities.filter { it.riskLevel == RiskLevel.LOW }
    assertTrue(lowRiskCaps.isNotEmpty())
  }
}
