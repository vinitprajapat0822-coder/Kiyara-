package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultDataSeeder
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

  @Test
  fun defaultSeederIsIdempotent() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val seeder = DefaultDataSeeder(database)
      seeder.seedIfNeeded()
      seeder.seedIfNeeded()

      assertEquals(1, database.chatDao().getMessageCount())
      assertEquals(1, database.memoryDao().getCountByCategoryAndTitle("JEE Prep", "Target Exam"))
      assertEquals(1, database.memoryDao().getCountByCategoryAndTitle("Preferences", "Language Style"))
    } finally {
      database.close()
    }
  }

  @Test
  fun defaultSeederPreservesExistingChatHistory() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      database.chatDao().insertMessage(
        ChatMessage(role = "user", content = "Keep this history")
      )

      DefaultDataSeeder(database).seedIfNeeded()

      val messages = database.chatDao().getAllMessages().first()
      assertEquals(1, messages.size)
      assertEquals("Keep this history", messages.single().content)
      assertEquals(1, database.memoryDao().getCountByCategoryAndTitle("JEE Prep", "Target Exam"))
    } finally {
      database.close()
    }
  }
}
