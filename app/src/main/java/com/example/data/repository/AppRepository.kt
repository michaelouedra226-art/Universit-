package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.CurriculumData
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AppRepository(private val database: AppDatabase, private val context: Context) {
    private val moduleDao = database.moduleDao()
    private val subTaskDao = database.subTaskDao()
    private val resourceDao = database.resourceDao()
    private val deadlineDao = database.deadlineDao()
    private val noteDao = database.noteDao()
    private val studySessionDao = database.studySessionDao()
    private val settingsDao = database.settingsDao()

    // Modules
    val allModules: Flow<List<CourseModule>> = moduleDao.getAllModules()

    suspend fun ensureInitialDataLoaded() {
        withContext(Dispatchers.IO) {
            val count = moduleDao.getModuleCount()
            if (count == 0) {
                moduleDao.insertAllModules(CurriculumData.initialModules)
                subTaskDao.insertAll(CurriculumData.initialSubTasks)
                settingsDao.saveSettings(UserSettings())
            }
        }
    }

    suspend fun getModuleById(id: Long): CourseModule? = moduleDao.getModuleById(id)

    suspend fun saveModule(module: CourseModule) {
        withContext(Dispatchers.IO) {
            if (module.id == 0L) {
                moduleDao.insertModule(module)
            } else {
                moduleDao.updateModule(module)
            }
        }
    }

    suspend fun updateProgression(moduleId: Long, progression: Int) {
        withContext(Dispatchers.IO) {
            val clamped = progression.coerceIn(0, 100)
            moduleDao.updateProgression(moduleId, clamped)
        }
    }

    suspend fun updateModuleNotes(moduleId: Long, notes: String) {
        withContext(Dispatchers.IO) {
            moduleDao.updateNotes(moduleId, notes)
        }
    }

    suspend fun deleteModule(module: CourseModule) {
        withContext(Dispatchers.IO) {
            subTaskDao.deleteByModule(module.id)
            resourceDao.deleteByModule(module.id)
            moduleDao.deleteModule(module)
        }
    }

    // Subtasks
    fun getSubTasksForModule(moduleId: Long): Flow<List<SubTask>> = subTaskDao.getSubTasksForModule(moduleId)

    suspend fun saveSubTask(subTask: SubTask) {
        withContext(Dispatchers.IO) {
            if (subTask.id == 0L) {
                subTaskDao.insertSubTask(subTask)
            } else {
                subTaskDao.updateSubTask(subTask)
            }
        }
    }

    suspend fun toggleSubTask(subTask: SubTask) {
        withContext(Dispatchers.IO) {
            subTaskDao.updateSubTask(subTask.copy(isCompleted = !subTask.isCompleted))
        }
    }

    suspend fun deleteSubTask(subTask: SubTask) {
        withContext(Dispatchers.IO) {
            subTaskDao.deleteSubTask(subTask)
        }
    }

    // Resources
    fun getResourcesForModule(moduleId: Long): Flow<List<ResourceLink>> = resourceDao.getResourcesForModule(moduleId)

    suspend fun addResource(resource: ResourceLink) {
        withContext(Dispatchers.IO) {
            resourceDao.insertResource(resource)
        }
    }

    suspend fun deleteResource(resource: ResourceLink) {
        withContext(Dispatchers.IO) {
            resourceDao.deleteResource(resource)
        }
    }

    // Deadlines
    val allDeadlines: Flow<List<Deadline>> = deadlineDao.getAllDeadlines()
    val upcomingDeadlines: Flow<List<Deadline>> = deadlineDao.getUpcomingDeadlines()

    suspend fun saveDeadline(deadline: Deadline) {
        withContext(Dispatchers.IO) {
            if (deadline.id == 0L) {
                deadlineDao.insertDeadline(deadline)
            } else {
                deadlineDao.updateDeadline(deadline)
            }
        }
    }

    suspend fun toggleDeadlineCompleted(deadline: Deadline) {
        withContext(Dispatchers.IO) {
            deadlineDao.updateDeadline(deadline.copy(isCompleted = !deadline.isCompleted))
        }
    }

    suspend fun deleteDeadline(deadline: Deadline) {
        withContext(Dispatchers.IO) {
            deadlineDao.deleteDeadline(deadline)
        }
    }

    // Free Notes
    val allNotes: Flow<List<FreeNote>> = noteDao.getAllNotes()

    suspend fun saveNote(note: FreeNote) {
        withContext(Dispatchers.IO) {
            val updated = note.copy(updatedAt = System.currentTimeMillis())
            if (updated.id == 0L) {
                noteDao.insertNote(updated)
            } else {
                noteDao.updateNote(updated)
            }
        }
    }

    suspend fun deleteNote(note: FreeNote) {
        withContext(Dispatchers.IO) {
            noteDao.deleteNote(note)
        }
    }

    // Study Sessions
    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()

    fun getSessionsForModule(moduleId: Long): Flow<List<StudySession>> = studySessionDao.getSessionsForModule(moduleId)

    fun getSessionsLastWeek(): Flow<List<StudySession>> {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return studySessionDao.getSessionsSince(calendar.timeInMillis)
    }

    suspend fun recordStudySession(moduleId: Long, durationSeconds: Int, mode: String, notes: String) {
        withContext(Dispatchers.IO) {
            if (durationSeconds > 0) {
                studySessionDao.insertSession(
                    StudySession(
                        moduleId = moduleId,
                        durationSeconds = durationSeconds,
                        mode = mode,
                        notes = notes,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Settings
    val settings: Flow<UserSettings?> = settingsDao.getSettings()

    suspend fun getSettingsSync(): UserSettings {
        return withContext(Dispatchers.IO) {
            settingsDao.getSettingsSync() ?: UserSettings()
        }
    }

    suspend fun saveSettings(userSettings: UserSettings) {
        withContext(Dispatchers.IO) {
            settingsDao.saveSettings(userSettings)
        }
    }

    // Backup & Restore
    suspend fun exportToJson(): String {
        return withContext(Dispatchers.IO) {
            val json = JSONObject()
            json.put("app", "GeoParcours UV-BF")
            json.put("version", "2.0")
            json.put("exportedAt", System.currentTimeMillis())
            json.put("dateString", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH).format(Date()))

            // Modules
            val modulesArray = JSONArray()
            val modules = database.openHelper.readableDatabase.let {
                // Read modules synchronously
                val list = mutableListOf<CourseModule>()
                val cursor = it.query("SELECT id, code, name, semester, credits, progression, notes, startDate, targetEndDate, isCustom, displayOrder FROM course_modules")
                while (cursor.moveToNext()) {
                    list.add(
                        CourseModule(
                            id = cursor.getLong(0),
                            code = cursor.getString(1),
                            name = cursor.getString(2),
                            semester = cursor.getInt(3),
                            credits = cursor.getInt(4),
                            progression = cursor.getInt(5),
                            notes = cursor.getString(6),
                            startDate = cursor.getLong(7),
                            targetEndDate = cursor.getLong(8),
                            isCustom = cursor.getInt(9) == 1,
                            displayOrder = cursor.getInt(10)
                        )
                    )
                }
                cursor.close()
                list
            }

            for (m in modules) {
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("code", m.code)
                obj.put("name", m.name)
                obj.put("semester", m.semester)
                obj.put("credits", m.credits)
                obj.put("progression", m.progression)
                obj.put("notes", m.notes)
                obj.put("startDate", m.startDate)
                obj.put("targetEndDate", m.targetEndDate)
                obj.put("isCustom", m.isCustom)
                obj.put("displayOrder", m.displayOrder)
                modulesArray.put(obj)
            }
            json.put("modules", modulesArray)

            // Subtasks
            val subTasks = subTaskDao.getAllSubTasks()
            val subTasksArray = JSONArray()
            for (st in subTasks) {
                val obj = JSONObject()
                obj.put("id", st.id)
                obj.put("moduleId", st.moduleId)
                obj.put("title", st.title)
                obj.put("isCompleted", st.isCompleted)
                obj.put("sortOrder", st.sortOrder)
                subTasksArray.put(obj)
            }
            json.put("subTasks", subTasksArray)

            // Resources
            val resources = resourceDao.getAllResources()
            val resArray = JSONArray()
            for (r in resources) {
                val obj = JSONObject()
                obj.put("id", r.id)
                obj.put("moduleId", r.moduleId)
                obj.put("title", r.title)
                obj.put("urlOrPath", r.urlOrPath)
                obj.put("type", r.type)
                resArray.put(obj)
            }
            json.put("resources", resArray)

            // Deadlines
            val deadlines = deadlineDao.getAllDeadlinesSync()
            val deadArray = JSONArray()
            for (d in deadlines) {
                val obj = JSONObject()
                obj.put("id", d.id)
                obj.put("title", d.title)
                obj.put("description", d.description)
                obj.put("dueDate", d.dueDate)
                obj.put("type", d.type)
                obj.put("moduleId", d.moduleId ?: -1L)
                obj.put("isCompleted", d.isCompleted)
                obj.put("reminderEnabled", d.reminderEnabled)
                deadArray.put(obj)
            }
            json.put("deadlines", deadArray)

            // Notes
            val notes = noteDao.getAllNotesSync()
            val notesArray = JSONArray()
            for (n in notes) {
                val obj = JSONObject()
                obj.put("id", n.id)
                obj.put("title", n.title)
                obj.put("content", n.content)
                obj.put("moduleId", n.moduleId ?: -1L)
                obj.put("updatedAt", n.updatedAt)
                notesArray.put(obj)
            }
            json.put("notes", notesArray)

            // Sessions
            val sessions = studySessionDao.getAllSessionsSync()
            val sessionsArray = JSONArray()
            for (s in sessions) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("moduleId", s.moduleId)
                obj.put("timestamp", s.timestamp)
                obj.put("durationSeconds", s.durationSeconds)
                obj.put("mode", s.mode)
                obj.put("notes", s.notes)
                sessionsArray.put(obj)
            }
            json.put("sessions", sessionsArray)

            // Settings
            val currentSettings = settingsDao.getSettingsSync() ?: UserSettings()
            val settingsObj = JSONObject()
            settingsObj.put("studentName", currentSettings.studentName)
            settingsObj.put("matricule", currentSettings.matricule)
            settingsObj.put("currentSemester", currentSettings.currentSemester)
            settingsObj.put("weeklyGoalHours", currentSettings.weeklyGoalHours)
            settingsObj.put("notificationsEnabled", currentSettings.notificationsEnabled)
            settingsObj.put("themeMode", currentSettings.themeMode)
            settingsObj.put("driveConnectedAccount", currentSettings.driveConnectedAccount ?: "")
            json.put("settings", settingsObj)

            json.toString(2)
        }
    }

    suspend fun restoreFromJson(jsonString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject(jsonString)
                val modulesArray = json.optJSONArray("modules") ?: JSONArray()
                val subTasksArray = json.optJSONArray("subTasks") ?: JSONArray()
                val resourcesArray = json.optJSONArray("resources") ?: JSONArray()
                val deadlinesArray = json.optJSONArray("deadlines") ?: JSONArray()
                val notesArray = json.optJSONArray("notes") ?: JSONArray()
                val sessionsArray = json.optJSONArray("sessions") ?: JSONArray()

                val modules = mutableListOf<CourseModule>()
                for (i in 0 until modulesArray.length()) {
                    val obj = modulesArray.getJSONObject(i)
                    modules.add(
                        CourseModule(
                            id = obj.optLong("id", 0L),
                            code = obj.optString("code", ""),
                            name = obj.optString("name", ""),
                            semester = obj.optInt("semester", 1),
                            credits = obj.optInt("credits", 4),
                            progression = obj.optInt("progression", 0),
                            notes = obj.optString("notes", ""),
                            startDate = obj.optLong("startDate", 0L),
                            targetEndDate = obj.optLong("targetEndDate", 0L),
                            isCustom = obj.optBoolean("isCustom", false),
                            displayOrder = obj.optInt("displayOrder", i)
                        )
                    )
                }

                val subTasks = mutableListOf<SubTask>()
                for (i in 0 until subTasksArray.length()) {
                    val obj = subTasksArray.getJSONObject(i)
                    subTasks.add(
                        SubTask(
                            id = obj.optLong("id", 0L),
                            moduleId = obj.optLong("moduleId", 1L),
                            title = obj.optString("title", ""),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            sortOrder = obj.optInt("sortOrder", i)
                        )
                    )
                }

                val resources = mutableListOf<ResourceLink>()
                for (i in 0 until resourcesArray.length()) {
                    val obj = resourcesArray.getJSONObject(i)
                    resources.add(
                        ResourceLink(
                            id = obj.optLong("id", 0L),
                            moduleId = obj.optLong("moduleId", 1L),
                            title = obj.optString("title", ""),
                            urlOrPath = obj.optString("urlOrPath", ""),
                            type = obj.optString("type", "cours")
                        )
                    )
                }

                val deadlines = mutableListOf<Deadline>()
                for (i in 0 until deadlinesArray.length()) {
                    val obj = deadlinesArray.getJSONObject(i)
                    val modId = obj.optLong("moduleId", -1L)
                    deadlines.add(
                        Deadline(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                            type = obj.optString("type", "Devoir"),
                            moduleId = if (modId > 0) modId else null,
                            isCompleted = obj.optBoolean("isCompleted", false),
                            reminderEnabled = obj.optBoolean("reminderEnabled", true)
                        )
                    )
                }

                val notes = mutableListOf<FreeNote>()
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    val modId = obj.optLong("moduleId", -1L)
                    notes.add(
                        FreeNote(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            moduleId = if (modId > 0) modId else null,
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }

                val sessions = mutableListOf<StudySession>()
                for (i in 0 until sessionsArray.length()) {
                    val obj = sessionsArray.getJSONObject(i)
                    sessions.add(
                        StudySession(
                            id = obj.optLong("id", 0L),
                            moduleId = obj.optLong("moduleId", 1L),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            durationSeconds = obj.optInt("durationSeconds", 0),
                            mode = obj.optString("mode", "CHRONO"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }

                database.withTransaction {
                    moduleDao.deleteAll()
                    subTaskDao.deleteAll()
                    resourceDao.deleteAll()
                    deadlineDao.deleteAll()
                    noteDao.deleteAll()
                    studySessionDao.deleteAll()

                    if (modules.isNotEmpty()) moduleDao.insertAllModules(modules)
                    if (subTasks.isNotEmpty()) subTaskDao.insertAll(subTasks)
                    if (resources.isNotEmpty()) resourceDao.insertAll(resources)
                    if (deadlines.isNotEmpty()) deadlineDao.insertAll(deadlines)
                    if (notes.isNotEmpty()) noteDao.insertAll(notes)
                    if (sessions.isNotEmpty()) studySessionDao.insertAll(sessions)

                    val current = settingsDao.getSettingsSync() ?: UserSettings()
                    settingsDao.saveSettings(
                        current.copy(
                            lastBackupTimestamp = System.currentTimeMillis(),
                            lastBackupStatus = "Restauration réussie (${SimpleDateFormat("dd/MM HH:mm", Locale.FRENCH).format(Date())})"
                        )
                    )
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun resetAllDataToDefault() {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                moduleDao.deleteAll()
                subTaskDao.deleteAll()
                resourceDao.deleteAll()
                deadlineDao.deleteAll()
                noteDao.deleteAll()
                studySessionDao.deleteAll()
                settingsDao.deleteAll()

                moduleDao.insertAllModules(CurriculumData.initialModules)
                subTaskDao.insertAll(CurriculumData.initialSubTasks)
                settingsDao.saveSettings(UserSettings())
            }
        }
    }
}
