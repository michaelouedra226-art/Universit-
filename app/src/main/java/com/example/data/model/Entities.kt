package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_modules")
data class CourseModule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val semester: Int, // 1 or 2
    val credits: Int,
    val progression: Int = 0, // 0 to 100
    val notes: String = "",
    val startDate: Long = 0L,
    val targetEndDate: Long = 0L,
    val isCustom: Boolean = false,
    val displayOrder: Int = 0
)

@Entity(tableName = "sub_tasks")
data class SubTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val moduleId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "resource_links")
data class ResourceLink(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val moduleId: Long,
    val title: String,
    val urlOrPath: String,
    val type: String = "cours" // "cours", "td", "tp", "externe"
)

@Entity(tableName = "deadlines")
data class Deadline(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long, // timestamp ms
    val type: String, // "Devoir", "Examen", "Forum", "Réinscription", "Autre"
    val moduleId: Long? = null,
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = true
)

@Entity(tableName = "free_notes")
data class FreeNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val moduleId: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val moduleId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val mode: String = "CHRONO", // "CHRONO" or "POMODORO"
    val notes: String = ""
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val studentName: String = "Moussa",
    val matricule: String = "UV-BF/L1-GEO/2026",
    val currentSemester: Int = 1,
    val weeklyGoalHours: Int = 20,
    val notificationsEnabled: Boolean = true,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val driveConnectedAccount: String? = null,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupStatus: String = "Aucune sauvegarde"
)
