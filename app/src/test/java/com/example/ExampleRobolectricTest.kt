package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CurriculumData
import com.example.data.repository.AppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GeoParcours UV-BF", appName)
    }

    @Test
    fun `verify curriculum initial modules count and structure`() {
        val modules = CurriculumData.initialModules
        assertEquals(14, modules.size)

        val s1Modules = modules.filter { it.semester == 1 }
        val s2Modules = modules.filter { it.semester == 2 }

        assertEquals(7, s1Modules.size)
        assertEquals(7, s2Modules.size)

        val totalCredits = modules.sumOf { it.credits }
        assertEquals(60, totalCredits)
    }

    @Test
    fun `verify database seeding and backup export`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = AppRepository(db, context)

        repo.ensureInitialDataLoaded()
        val json = repo.exportToJson()

        assertTrue(json.contains("GeoParcours UV-BF"))
        assertTrue(json.contains("GEO101"))
        assertTrue(json.contains("GEO102"))
    }
}
