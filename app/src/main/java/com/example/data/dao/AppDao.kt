package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM course_modules ORDER BY semester ASC, displayOrder ASC, id ASC")
    fun getAllModules(): Flow<List<CourseModule>>

    @Query("SELECT * FROM course_modules WHERE id = :id LIMIT 1")
    suspend fun getModuleById(id: Long): CourseModule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: CourseModule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllModules(modules: List<CourseModule>)

    @Update
    suspend fun updateModule(module: CourseModule)

    @Delete
    suspend fun deleteModule(module: CourseModule)

    @Query("UPDATE course_modules SET progression = :progression WHERE id = :id")
    suspend fun updateProgression(id: Long, progression: Int)

    @Query("UPDATE course_modules SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Query("SELECT COUNT(*) FROM course_modules")
    suspend fun getModuleCount(): Int

    @Query("DELETE FROM course_modules")
    suspend fun deleteAll()
}

@Dao
interface SubTaskDao {
    @Query("SELECT * FROM sub_tasks WHERE moduleId = :moduleId ORDER BY sortOrder ASC, id ASC")
    fun getSubTasksForModule(moduleId: Long): Flow<List<SubTask>>

    @Query("SELECT * FROM sub_tasks")
    suspend fun getAllSubTasks(): List<SubTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(task: SubTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<SubTask>)

    @Update
    suspend fun updateSubTask(task: SubTask)

    @Delete
    suspend fun deleteSubTask(task: SubTask)

    @Query("DELETE FROM sub_tasks WHERE moduleId = :moduleId")
    suspend fun deleteByModule(moduleId: Long)

    @Query("DELETE FROM sub_tasks")
    suspend fun deleteAll()
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resource_links WHERE moduleId = :moduleId ORDER BY id DESC")
    fun getResourcesForModule(moduleId: Long): Flow<List<ResourceLink>>

    @Query("SELECT * FROM resource_links")
    suspend fun getAllResources(): List<ResourceLink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceLink): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resources: List<ResourceLink>)

    @Delete
    suspend fun deleteResource(resource: ResourceLink)

    @Query("DELETE FROM resource_links WHERE moduleId = :moduleId")
    suspend fun deleteByModule(moduleId: Long)

    @Query("DELETE FROM resource_links")
    suspend fun deleteAll()
}

@Dao
interface DeadlineDao {
    @Query("SELECT * FROM deadlines ORDER BY dueDate ASC")
    fun getAllDeadlines(): Flow<List<Deadline>>

    @Query("SELECT * FROM deadlines WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getUpcomingDeadlines(): Flow<List<Deadline>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeadline(deadline: Deadline): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deadlines: List<Deadline>)

    @Update
    suspend fun updateDeadline(deadline: Deadline)

    @Delete
    suspend fun deleteDeadline(deadline: Deadline)

    @Query("SELECT * FROM deadlines")
    suspend fun getAllDeadlinesSync(): List<Deadline>

    @Query("DELETE FROM deadlines")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM free_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<FreeNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: FreeNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<FreeNote>)

    @Update
    suspend fun updateNote(note: FreeNote)

    @Delete
    suspend fun deleteNote(note: FreeNote)

    @Query("SELECT * FROM free_notes")
    suspend fun getAllNotesSync(): List<FreeNote>

    @Query("DELETE FROM free_notes")
    suspend fun deleteAll()
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE moduleId = :moduleId ORDER BY timestamp DESC")
    fun getSessionsForModule(moduleId: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<StudySession>)

    @Query("SELECT * FROM study_sessions")
    suspend fun getAllSessionsSync(): List<StudySession>

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettings)

    @Query("DELETE FROM user_settings")
    suspend fun deleteAll()
}
