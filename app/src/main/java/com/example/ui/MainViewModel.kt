package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class TimerMode {
    CHRONO,
    POMODORO
}

enum class PomodoroPhase {
    WORK,
    BREAK
}

data class BackupVersion(
    val fileName: String,
    val dateString: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val jsonContent: String
)

data class DayStudyStat(
    val dayLabel: String, // "Lun", "Mar", etc.
    val minutes: Int,
    val isToday: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = AppRepository(database, application)

    // Data streams
    val modules: StateFlow<List<CourseModule>> = repository.allModules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deadlines: StateFlow<List<Deadline>> = repository.allDeadlines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<FreeNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = repository.settings
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    // Timer State
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds = _timerSeconds.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.CHRONO)
    val timerMode = _timerMode.asStateFlow()

    private val _pomodoroPhase = MutableStateFlow(PomodoroPhase.WORK)
    val pomodoroPhase = _pomodoroPhase.asStateFlow()

    private val _pomodoroWorkMinutes = MutableStateFlow(25)
    val pomodoroWorkMinutes = _pomodoroWorkMinutes.asStateFlow()

    private val _pomodoroBreakMinutes = MutableStateFlow(5)
    val pomodoroBreakMinutes = _pomodoroBreakMinutes.asStateFlow()

    private val _selectedTimerModuleId = MutableStateFlow<Long?>(null)
    val selectedTimerModuleId = _selectedTimerModuleId.asStateFlow()

    private var timerJob: Job? = null

    // Backup Versions state
    private val _backupVersions = MutableStateFlow<List<BackupVersion>>(emptyList())
    val backupVersions = _backupVersions.asStateFlow()

    // Daily quote
    val dailyMotivation: String
        get() {
            val quotes = listOf(
                "« Petit à petit, l'oiseau fait son nid. » — La régularité dans vos modules géomatiques garantit la réussite.",
                "« L'eau chaude n'oublie jamais qu'elle a été froide. » — Chaque expert SIG a commencé par la base des projections.",
                "« La géomatique est l'œil qui révèle le territoire et guide le développement du Faso. »",
                "« La persévérance est la clé des études universitaires à distance à l'UV-BF. »",
                "« Mesurer avec précision, cartographier avec rigueur, servir avec fierté. »"
            )
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            return quotes[dayOfYear % quotes.size]
        }

    init {
        viewModelScope.launch {
            repository.ensureInitialDataLoaded()
            loadLocalBackups()
        }
    }

    // Progression globale pondérée par crédits
    val globalProgress = modules.map { mods ->
        val totalCredits = mods.sumOf { it.credits }
        if (totalCredits == 0) {
            0
        } else {
            val weightedSum = mods.sumOf { it.progression * it.credits }
            (weightedSum / totalCredits.toDouble()).toInt().coerceIn(0, 100)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedModulesCount = modules.map { mods ->
        mods.count { it.progression >= 100 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Modules prioritaires (score calculé)
    val priorityModules = combine(modules, deadlines, sessions) { mods, dls, sess ->
        val now = System.currentTimeMillis()
        mods.filter { it.progression < 100 }
            .map { mod ->
                var score = (100 - mod.progression) * 1.0

                // Proximité de la prochaine échéance
                val upcomingDeadline = dls.filter { !it.isCompleted && it.moduleId == mod.id && it.dueDate >= now }
                    .minByOrNull { it.dueDate }
                if (upcomingDeadline != null) {
                    val daysUntil = ((upcomingDeadline.dueDate - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                    score += when {
                        daysUntil <= 2 -> 50.0
                        daysUntil <= 7 -> 25.0
                        else -> 10.0
                    }
                }

                // Jours sans session d'étude sur ce module
                val lastSession = sess.filter { it.moduleId == mod.id }.maxByOrNull { it.timestamp }
                if (lastSession != null) {
                    val daysSince = ((now - lastSession.timestamp) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                    score += (daysSince * 2).coerceAtMost(30)
                } else {
                    score += 20.0
                }

                Pair(mod, score)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly study stats
    val weeklyStudyStats = sessions.map { sessList ->
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        val dayFormat = SimpleDateFormat("EEE", Locale.FRENCH)
        val stats = mutableListOf<DayStudyStat>()

        // 7 derniers jours (du 6e jour avant aujourd'hui jusqu'à aujourd'hui)
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startOfDay = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val totalSecs = sessList.filter { it.timestamp in startOfDay..endOfDay }
                .sumOf { it.durationSeconds }

            val label = dayFormat.format(Date(startOfDay)).replace(".", "").replaceFirstChar { it.uppercase() }
            stats.add(DayStudyStat(dayLabel = label, minutes = totalSecs / 60, isToday = (i == 0)))
        }
        stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWeekMinutes = weeklyStudyStats.map { list ->
        list.sumOf { it.minutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val mostStudiedModuleName = combine(sessions, modules) { sessList, modMap ->
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val weekSessions = sessList.filter { it.timestamp >= calendar.timeInMillis }
        val grouped = weekSessions.groupBy { it.moduleId }
        val topEntry = grouped.maxByOrNull { entry -> entry.value.sumOf { it.durationSeconds } }
        if (topEntry != null) {
            modMap.find { it.id == topEntry.key }?.code ?: "Module #${topEntry.key}"
        } else {
            "Aucun"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Aucun")

    // Timer Controls
    fun setTimerMode(mode: TimerMode) {
        if (_isTimerRunning.value) pauseTimer()
        _timerMode.value = mode
        if (mode == TimerMode.POMODORO) {
            _pomodoroPhase.value = PomodoroPhase.WORK
            _timerSeconds.value = _pomodoroWorkMinutes.value * 60
        } else {
            _timerSeconds.value = 0
        }
    }

    fun setSelectedTimerModule(moduleId: Long?) {
        _selectedTimerModuleId.value = moduleId
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000)
                if (_timerMode.value == TimerMode.CHRONO) {
                    _timerSeconds.value += 1
                } else {
                    // Pomodoro countdown
                    if (_timerSeconds.value > 0) {
                        _timerSeconds.value -= 1
                    } else {
                        // Switch phase
                        if (_pomodoroPhase.value == PomodoroPhase.WORK) {
                            _pomodoroPhase.value = PomodoroPhase.BREAK
                            _timerSeconds.value = _pomodoroBreakMinutes.value * 60
                        } else {
                            _pomodoroPhase.value = PomodoroPhase.WORK
                            _timerSeconds.value = _pomodoroWorkMinutes.value * 60
                        }
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        if (_timerMode.value == TimerMode.CHRONO) {
            _timerSeconds.value = 0
        } else {
            _pomodoroPhase.value = PomodoroPhase.WORK
            _timerSeconds.value = _pomodoroWorkMinutes.value * 60
        }
    }

    fun finishAndSaveTimerSession(notes: String = "", onSaved: () -> Unit = {}) {
        val modId = _selectedTimerModuleId.value ?: return
        val currentSecs = if (_timerMode.value == TimerMode.CHRONO) {
            _timerSeconds.value
        } else {
            (_pomodoroWorkMinutes.value * 60) - _timerSeconds.value
        }.coerceAtLeast(0)

        if (currentSecs >= 30) { // save if at least 30s
            viewModelScope.launch {
                repository.recordStudySession(
                    moduleId = modId,
                    durationSeconds = currentSecs,
                    mode = _timerMode.value.name,
                    notes = notes
                )
                resetTimer()
                onSaved()
            }
        } else {
            resetTimer()
            onSaved()
        }
    }

    // Module actions
    fun updateModuleProgression(moduleId: Long, progression: Int) {
        viewModelScope.launch {
            repository.updateProgression(moduleId, progression)
        }
    }

    fun updateModuleNotes(moduleId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateModuleNotes(moduleId, notes)
        }
    }

    fun saveModule(module: CourseModule) {
        viewModelScope.launch {
            repository.saveModule(module)
        }
    }

    fun deleteModule(module: CourseModule) {
        viewModelScope.launch {
            repository.deleteModule(module)
        }
    }

    // Subtasks actions
    fun getSubTasksForModule(moduleId: Long): Flow<List<SubTask>> = repository.getSubTasksForModule(moduleId)

    fun addSubTask(moduleId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.saveSubTask(SubTask(moduleId = moduleId, title = title.trim()))
        }
    }

    fun toggleSubTask(subTask: SubTask) {
        viewModelScope.launch {
            repository.toggleSubTask(subTask)
        }
    }

    fun deleteSubTask(subTask: SubTask) {
        viewModelScope.launch {
            repository.deleteSubTask(subTask)
        }
    }

    // Resources actions
    fun getResourcesForModule(moduleId: Long): Flow<List<ResourceLink>> = repository.getResourcesForModule(moduleId)

    fun addResource(moduleId: Long, title: String, urlOrPath: String, type: String = "cours") {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addResource(
                ResourceLink(
                    moduleId = moduleId,
                    title = title.trim(),
                    urlOrPath = urlOrPath.trim(),
                    type = type
                )
            )
        }
    }

    fun deleteResource(resource: ResourceLink) {
        viewModelScope.launch {
            repository.deleteResource(resource)
        }
    }

    // Deadlines actions
    fun saveDeadline(deadline: Deadline) {
        viewModelScope.launch {
            repository.saveDeadline(deadline)
        }
    }

    fun toggleDeadline(deadline: Deadline) {
        viewModelScope.launch {
            repository.toggleDeadlineCompleted(deadline)
        }
    }

    fun deleteDeadline(deadline: Deadline) {
        viewModelScope.launch {
            repository.deleteDeadline(deadline)
        }
    }

    // Notes actions
    fun saveNote(note: FreeNote) {
        viewModelScope.launch {
            repository.saveNote(note)
        }
    }

    fun deleteNote(note: FreeNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Settings actions
    fun updateStudentProfile(name: String, matricule: String, currentSemester: Int, weeklyGoal: Int) {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            repository.saveSettings(
                current.copy(
                    studentName = name.trim(),
                    matricule = matricule.trim(),
                    currentSemester = currentSemester,
                    weeklyGoalHours = weeklyGoal
                )
            )
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            repository.saveSettings(current.copy(themeMode = themeMode))
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            repository.saveSettings(current.copy(notificationsEnabled = enabled))
        }
    }

    // Google Drive & Local Backups
    private val backupDir by lazy {
        File(getApplication<Application>().filesDir, "backups").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun loadLocalBackups() {
        val files = backupDir.listFiles() ?: emptyArray()
        val list = files.filter { it.extension == "json" }
            .sortedByDescending { it.lastModified() }
            .take(5)
            .map { file ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date(file.lastModified()))
                BackupVersion(
                    fileName = file.name,
                    dateString = dateStr,
                    timestamp = file.lastModified(),
                    sizeBytes = file.length(),
                    jsonContent = file.readText()
                )
            }
        _backupVersions.value = list
    }

    fun performBackup(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val json = repository.exportToJson()
                val timestamp = System.currentTimeMillis()
                val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRENCH).format(Date(timestamp))
                val fileName = "geoparcours_backup_$timeStr.json"
                val file = File(backupDir, fileName)
                file.writeText(json)

                // Maintain 5 latest versions
                val allFiles = backupDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
                if (allFiles.size > 5) {
                    allFiles.drop(5).forEach { it.delete() }
                }

                val current = repository.getSettingsSync()
                val statusText = "Sauvegarde réussie (${SimpleDateFormat("dd/MM HH:mm", Locale.FRENCH).format(Date(timestamp))})"
                repository.saveSettings(
                    current.copy(
                        lastBackupTimestamp = timestamp,
                        lastBackupStatus = statusText
                    )
                )
                loadLocalBackups()
                onComplete(true, "Sauvegarde enregistrée avec succès dans le dossier GeoParcours UV-BF.")
            } catch (e: Exception) {
                val current = repository.getSettingsSync()
                repository.saveSettings(current.copy(lastBackupStatus = "Échec de la sauvegarde"))
                onComplete(false, "Erreur lors de la sauvegarde : ${e.localizedMessage}")
            }
        }
    }

    fun restoreBackup(version: BackupVersion, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreFromJson(version.jsonContent)
            loadLocalBackups()
            onComplete(success)
        }
    }

    fun restoreFromJsonText(rawJson: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreFromJson(rawJson)
            loadLocalBackups()
            onComplete(success)
        }
    }

    fun connectGoogleDrive(accountEmail: String) {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            repository.saveSettings(
                current.copy(
                    driveConnectedAccount = accountEmail,
                    lastBackupStatus = "Connecté à Google Drive (Dossier 'GeoParcours UV-BF' prêt)"
                )
            )
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            val current = repository.getSettingsSync()
            repository.saveSettings(
                current.copy(
                    driveConnectedAccount = null,
                    lastBackupStatus = "Déconnecté de Google Drive"
                )
            )
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resetAllDataToDefault()
            loadLocalBackups()
            onComplete()
        }
    }
}
