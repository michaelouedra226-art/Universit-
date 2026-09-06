package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var studentName by remember(settings.studentName) { mutableStateOf(settings.studentName) }
    var matricule by remember(settings.matricule) { mutableStateOf(settings.matricule) }
    var currentSemester by remember(settings.currentSemester) { mutableIntStateOf(settings.currentSemester) }
    var weeklyGoalHours by remember(settings.weeklyGoalHours) { mutableIntStateOf(settings.weeklyGoalHours) }
    var notificationsEnabled by remember(settings.notificationsEnabled) { mutableStateOf(settings.notificationsEnabled) }

    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profil Étudiant
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Profil Étudiant UV-BF",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Prénom & Nom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = matricule,
                        onValueChange = { matricule = it },
                        label = { Text("Matricule UV-BF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Semestre en cours :",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = currentSemester == 1,
                            onClick = { currentSemester = 1 },
                            label = { Text("Semestre 1 (Harmattan)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentSemester == 2,
                            onClick = { currentSemester = 2 },
                            label = { Text("Semestre 2 (Mousson)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Objectif hebdomadaire : $weeklyGoalHours heures / semaine",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Slider(
                        value = weeklyGoalHours.toFloat(),
                        onValueChange = { weeklyGoalHours = it.toInt() },
                        valueRange = 5f..40f,
                        steps = 34,
                        colors = SliderDefaults.colors(
                            thumbColor = OrangeTerreCuite,
                            activeTrackColor = OrangeTerreCuite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.updateStudentProfile(
                                name = studentName,
                                matricule = matricule,
                                currentSemester = currentSemester,
                                weeklyGoal = weeklyGoalHours
                            )
                            Toast.makeText(context, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer les modifications")
                    }
                }
            }
        }

        // Préférences d'affichage et notifications
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Préférences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Thème de l'application :", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "Auto", "LIGHT" to "Clair", "DARK" to "Sombre").forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rappels et alertes d'échéances",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Alertes locales 48h et 24h avant les devoirs",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                viewModel.updateNotifications(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = VertEmeraude, checkedTrackColor = VertEmeraudeContainerLight)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Raccourci vers la sauvegarde Google Drive
                    OutlinedButton(
                        onClick = onNavigateToBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gestion des Sauvegardes Google Drive")
                    }
                }
            }
        }

        // À propos & Crédits académiques
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "À Propos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GeoParcours UV-BF • Version 2.0 Production",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = VertEmeraude)
                    )
                    Text(
                        text = "Application d'accompagnement académique conçue spécialement pour la Licence 1 en Géomatique de l'Université Virtuelle du Burkina Faso (UV-BF).",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mode 100% Hors-ligne avec synchronisation Google Drive optionnelle. Conforme au référentiel LMD UV-BF.",
                        style = MaterialTheme.typography.labelSmall.copy(color = GrisNeutre)
                    )
                }
            }
        }

        // Zone de Danger (Réinitialisation)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatutEnRetard.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Zone de danger",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = StatutEnRetard)
                    )
                    Text(
                        text = "Réinitialise tous les modules, devoirs, notes et chronomètres à l'état initial du programme UV-BF.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StatutEnRetard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Réinitialiser toutes les données")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirmation requise", fontWeight = FontWeight.Bold, color = StatutEnRetard) },
            text = {
                Text(
                    "Êtes-vous absolument sûr de vouloir réinitialiser l'application ? Toutes vos progressions personnalisées, notes et sessions d'étude seront effacées et remplacées par le programme initial UV-BF."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData {
                            showResetDialog = false
                            Toast.makeText(context, "Application réinitialisée au programme initial", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatutEnRetard)
                ) {
                    Text("Oui, réinitialiser tout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
