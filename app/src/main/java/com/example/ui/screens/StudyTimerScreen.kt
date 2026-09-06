package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseModule
import com.example.ui.MainViewModel
import com.example.ui.PomodoroPhase
import com.example.ui.TimerMode
import com.example.ui.components.WeeklyStudyBarChart
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTimerScreen(
    viewModel: MainViewModel,
    preselectedModuleId: Long? = null,
    modifier: Modifier = Modifier
) {
    val modules by viewModel.modules.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val pomodoroPhase by viewModel.pomodoroPhase.collectAsState()
    val selectedModuleId by viewModel.selectedTimerModuleId.collectAsState()

    val weeklyStats by viewModel.weeklyStudyStats.collectAsState()
    val totalWeekMinutes by viewModel.totalWeekMinutes.collectAsState()
    val mostStudiedModuleName by viewModel.mostStudiedModuleName.collectAsState()

    var sessionNotes by remember { mutableStateOf("") }
    var moduleDropdownExpanded by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(preselectedModuleId) {
        if (preselectedModuleId != null && selectedModuleId == null) {
            viewModel.setSelectedTimerModule(preselectedModuleId)
        } else if (selectedModuleId == null && modules.isNotEmpty()) {
            viewModel.setSelectedTimerModule(modules.first().id)
        }
    }

    val selectedModule = modules.find { it.id == selectedModuleId }

    // Format mm:ss or hh:mm:ss
    val formattedTime = remember(timerSeconds) {
        val hours = timerSeconds / 3600
        val minutes = (timerSeconds % 3600) / 60
        val seconds = timerSeconds % 60
        if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("study_timer_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête sélecteur de mode (Chrono vs Pomodoro)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = timerMode == TimerMode.CHRONO,
                            onClick = { viewModel.setTimerMode(TimerMode.CHRONO) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Chronomètre")
                        }
                        SegmentedButton(
                            selected = timerMode == TimerMode.POMODORO,
                            onClick = { viewModel.setTimerMode(TimerMode.POMODORO) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Pomodoro (25/5)")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sélection obligatoire du module
                    Text(
                        text = "Module d'étude associé :",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = moduleDropdownExpanded,
                        onExpandedChange = { if (!isRunning) moduleDropdownExpanded = !moduleDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModule?.let { "${it.code} - ${it.name}" } ?: "Sélectionner un module",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moduleDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isRunning
                        )

                        ExposedDropdownMenu(
                            expanded = moduleDropdownExpanded,
                            onDismissRequest = { moduleDropdownExpanded = false }
                        ) {
                            modules.forEach { mod ->
                                DropdownMenuItem(
                                    text = { Text("${mod.code} - ${mod.name}") },
                                    onClick = {
                                        viewModel.setSelectedTimerModule(mod.id)
                                        moduleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horloge circulaire / Affichage du temps
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (timerMode == TimerMode.POMODORO) {
                        Surface(
                            color = if (pomodoroPhase == PomodoroPhase.WORK) OrangeContainerLight else VertEmeraudeContainerLight,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (pomodoroPhase == PomodoroPhase.WORK) "SESSION DE TRAVAIL (25 min)" else "PAUSE (5 min)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (pomodoroPhase == PomodoroPhase.WORK) OrangeTerreCuite else VertEmeraude
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Grande bulle de temps
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isRunning) VertEmeraude.copy(alpha = 0.15f) else GrisClair.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning) VertEmeraude else MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 2.sp
                                )
                            )
                            Text(
                                text = if (isRunning) "En cours..." else "Prêt",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Commandes de contrôle (Démarrer, Pause, Réinitialiser, Sauvegarder)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedIconButton(
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Réinitialiser")
                        }

                        Button(
                            onClick = {
                                if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) OrangeTerreCuite else VertEmeraude
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Démarrer",
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(VertEmeraudeContainerLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Enregistrer la session",
                                tint = VertEmeraude
                            )
                        }
                    }
                }
            }
        }

        // Statistiques de la semaine
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Activité des 7 derniers jours",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    WeeklyStudyBarChart(stats = weeklyStats)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total semaine",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            val h = totalWeekMinutes / 60
                            val m = totalWeekMinutes % 60
                            Text(
                                text = "${h}h ${m}m",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = VertEmeraude)
                            )
                        }

                        Column {
                            Text(
                                text = "Moyenne / jour",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            val dailyAvg = totalWeekMinutes / 7
                            Text(
                                text = "${dailyAvg} min/jour",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OrangeTerreCuite)
                            )
                        }

                        Column {
                            Text(
                                text = "Top module",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = mostStudiedModuleName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrunTerre)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Enregistrer la session", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Module : ${selectedModule?.code} (${selectedModule?.name})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Temps : $formattedTime",
                        style = MaterialTheme.typography.bodySmall.copy(color = VertEmeraude)
                    )
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        label = { Text("Commentaire / Bilan de session (optionnel)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishAndSaveTimerSession(notes = sessionNotes) {
                            showSaveDialog = false
                            sessionNotes = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude)
                ) {
                    Text("Valider et Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Annuler") }
            }
        )
    }
}
