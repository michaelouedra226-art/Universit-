package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseModule
import com.example.data.model.Deadline
import com.example.ui.MainViewModel
import com.example.ui.components.AcademicCircularProgress
import com.example.ui.components.AfricanPatternBar
import com.example.ui.components.ModuleStatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToModule: (Long) -> Unit,
    onNavigateToDeadlines: () -> Unit,
    onNavigateToTimer: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val modules by viewModel.modules.collectAsState()
    val deadlines by viewModel.deadlines.collectAsState()
    val globalProgress by viewModel.globalProgress.collectAsState()
    val completedCount by viewModel.completedModulesCount.collectAsState()
    val priorityModules by viewModel.priorityModules.collectAsState()
    val totalWeekMinutes by viewModel.totalWeekMinutes.collectAsState()
    val dailyMotivation = viewModel.dailyMotivation

    val upcomingDeadlines = remember(deadlines) {
        val now = System.currentTimeMillis()
        deadlines.filter { !it.isCompleted }
            .sortedBy { it.dueDate }
            .take(3)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // En-tête de bienvenue avec motif africain subtil
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    AfricanPatternBar()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "UV-BF • Licence 1",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = VertEmeraude,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bonjour, ${settings.studentName}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Géomatique • Semestre ${settings.currentSemester}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        // Logo / Avatar insigne
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(BeigeSable),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Logo Géomatique",
                                tint = VertEmeraude,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }

        // Citation quotidienne de motivation
        item {
            Surface(
                color = OrangeContainerLight.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Motivation",
                        tint = OrangeTerreCuite,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dailyMotivation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = BrunTerre
                        )
                    )
                }
            }
        }

        // Progression Globale et Compteur de modules terminés
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Progression Académique Globale",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Moyenne pondérée par les crédits ECTS",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AcademicCircularProgress(
                            progressPercent = globalProgress,
                            sizeDp = 130.dp,
                            strokeWidth = 12.dp,
                            label = "Année L1"
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Surface(
                                color = VertEmeraudeContainerLight,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = VertEmeraude,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "$completedCount / ${modules.size}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = VertEmeraude
                                            )
                                        )
                                        Text(
                                            text = "Modules validés",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = BrunTerre
                                            )
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = BeigeSable,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = BrunTerre,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        val totalCredits = modules.sumOf { it.credits }
                                        val acquiredCredits = modules.filter { it.progression >= 100 }.sumOf { it.credits }
                                        Text(
                                            text = "$acquiredCredits / $totalCredits ECTS",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BrunTerre
                                            )
                                        )
                                        Text(
                                            text = "Crédits acquis",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = BrunTerre
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Temps d'étude cette semaine
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Temps d'Étude Cette Semaine",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            val hours = totalWeekMinutes / 60
                            val mins = totalWeekMinutes % 60
                            Text(
                                text = "${hours}h ${mins}m réalisés sur ${settings.weeklyGoalHours}h prévues",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        IconButton(
                            onClick = { onNavigateToTimer(null) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(VertEmeraude)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Démarrer session",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val goalMinutes = (settings.weeklyGoalHours * 60).coerceAtLeast(60)
                    val progressFraction = (totalWeekMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = OrangeTerreCuite,
                        trackColor = GrisClair
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(progressFraction * 100).toInt()}% de l'objectif hebdomadaire",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = OrangeTerreCuite
                        )
                    )
                }
            }
        }

        // Prochaines échéances (Deadlines)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prochaines Échéances",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                TextButton(onClick = onNavigateToDeadlines) {
                    Text(
                        text = "Voir tout",
                        color = VertEmeraude,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (upcomingDeadlines.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = VertEmeraude,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucune échéance urgente en attente.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    upcomingDeadlines.forEach { dl ->
                        DeadlineDashboardItem(
                            deadline = dl,
                            moduleCode = modules.find { it.id == dl.moduleId }?.code,
                            onToggle = { viewModel.toggleDeadline(dl) }
                        )
                    }
                }
            }
        }

        // Modules prioritaires (Recommandation intelligente)
        item {
            Column {
                Text(
                    text = "Modules Prioritaires à Réviser",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Basé sur l'avancement, les dates limites et l'inactivité",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val priorityList = priorityModules.take(5)
            if (priorityList.isEmpty()) {
                Text(
                    text = "Tous vos modules sont terminés à 100% !",
                    style = MaterialTheme.typography.bodyMedium.copy(color = VertEmeraude, fontWeight = FontWeight.Bold)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(priorityList, key = { it.id }) { mod ->
                        PriorityModuleCard(
                            module = mod,
                            onClick = { onNavigateToModule(mod.id) },
                            onStartTimer = { onNavigateToTimer(mod.id) }
                        )
                    }
                }
            }
        }

        // Espacement final
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun DeadlineDashboardItem(
    deadline: Deadline,
    moduleCode: String?,
    onToggle: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isOverdue = deadline.dueDate < now
    val diffHours = ((deadline.dueDate - now) / (1000 * 60 * 60))
    val isUrgent = diffHours in 0..48

    val dateFormat = SimpleDateFormat("dd MMMM à HH:mm", Locale.FRENCH)
    val dateText = dateFormat.format(Date(deadline.dueDate))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverdue -> StatutEnRetard.copy(alpha = 0.08f)
                isUrgent -> OrangeContainerLight.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (deadline.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Marquer comme fait",
                    tint = if (deadline.isCompleted) VertEmeraude else GrisNeutre
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (moduleCode != null) {
                        Surface(
                            color = BeigeSable,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = moduleCode,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrunTerre
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = deadline.type,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = OrangeTerreCuite,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = deadline.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isOverdue) StatutEnRetard else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isOverdue || isUrgent) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            if (isOverdue) {
                Surface(
                    color = StatutEnRetard,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Retard",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (isUrgent) {
                Surface(
                    color = OrangeTerreCuite,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Urgent",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityModuleCard(
    module: CourseModule,
    onClick: () -> Unit,
    onStartTimer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = VertEmeraudeContainerLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = module.code,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VertEmeraude
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${module.credits} ECTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = module.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${module.progression}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = VertEmeraude
                    )
                )
                IconButton(
                    onClick = onStartTimer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Chrono",
                        tint = OrangeTerreCuite
                    )
                }
            }

            LinearProgressIndicator(
                progress = { module.progression / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = VertEmeraude,
                trackColor = GrisClair
            )
        }
    }
}
