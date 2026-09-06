package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AfricanPatternBar
import com.example.ui.screens.*
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Accueil", Icons.Default.Dashboard)
    data object Modules : Screen("modules", "Modules", Icons.Default.MenuBook)
    data object Deadlines : Screen("deadlines", "Échéances", Icons.Default.Event)
    data object Timer : Screen("timer", "Étude", Icons.Default.Timer)
    data object Hub : Screen("hub", "Plus", Icons.Default.Widgets)
}

sealed class SubScreen {
    data object None : SubScreen()
    data class ModuleDetail(val moduleId: Long) : SubScreen()
    data object Backup : SubScreen()
    data object Notes : SubScreen()
    data object Settings : SubScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoParcoursApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var currentSubScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
    var preselectedTimerModuleId by remember { mutableStateOf<Long?>(null) }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Modules,
        Screen.Deadlines,
        Screen.Timer,
        Screen.Hub
    )

    Scaffold(
        topBar = {
            if (currentSubScreen == SubScreen.None) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "GeoParcours UV-BF",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VertEmeraude
                                )
                            )
                            Text(
                                text = currentTab.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentSubScreen = SubScreen.Settings }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profil",
                                tint = VertEmeraude
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (currentSubScreen == SubScreen.None) {
                Column {
                    AfricanPatternBar(height = 3.dp)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                selected = currentTab == screen,
                                onClick = { currentTab = screen },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontWeight = if (currentTab == screen) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VertEmeraude,
                                    selectedTextColor = VertEmeraude,
                                    indicatorColor = VertEmeraudeContainerLight
                                )
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val sub = currentSubScreen) {
                is SubScreen.ModuleDetail -> {
                    ModuleDetailScreen(
                        moduleId = sub.moduleId,
                        viewModel = viewModel,
                        onBack = { currentSubScreen = SubScreen.None },
                        onStartTimer = { modId ->
                            preselectedTimerModuleId = modId
                            currentSubScreen = SubScreen.None
                            currentTab = Screen.Timer
                        }
                    )
                }

                is SubScreen.Backup -> {
                    Column {
                        TopAppBar(
                            title = { Text("Sauvegarde Google Drive", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubScreen = SubScreen.None }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
                                }
                            }
                        )
                        BackupScreen(viewModel = viewModel)
                    }
                }

                is SubScreen.Notes -> {
                    Column {
                        TopAppBar(
                            title = { Text("Notes de cours & libres", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubScreen = SubScreen.None }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
                                }
                            }
                        )
                        NotesScreen(viewModel = viewModel)
                    }
                }

                is SubScreen.Settings -> {
                    Column {
                        TopAppBar(
                            title = { Text("Paramètres & Profil", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { currentSubScreen = SubScreen.None }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
                                }
                            }
                        )
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToBackup = { currentSubScreen = SubScreen.Backup }
                        )
                    }
                }

                is SubScreen.None -> {
                    Crossfade(targetState = currentTab, label = "tabTransition") { screen ->
                        when (screen) {
                            Screen.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToModule = { modId ->
                                    currentSubScreen = SubScreen.ModuleDetail(modId)
                                },
                                onNavigateToDeadlines = { currentTab = Screen.Deadlines },
                                onNavigateToTimer = { modId ->
                                    preselectedTimerModuleId = modId
                                    currentTab = Screen.Timer
                                }
                            )

                            Screen.Modules -> ModulesScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { modId ->
                                    currentSubScreen = SubScreen.ModuleDetail(modId)
                                }
                            )

                            Screen.Deadlines -> DeadlinesScreen(
                                viewModel = viewModel
                            )

                            Screen.Timer -> StudyTimerScreen(
                                viewModel = viewModel,
                                preselectedModuleId = preselectedTimerModuleId
                            )

                            Screen.Hub -> HubScreen(
                                viewModel = viewModel,
                                onNavigateToNotes = { currentSubScreen = SubScreen.Notes },
                                onNavigateToBackup = { currentSubScreen = SubScreen.Backup },
                                onNavigateToSettings = { currentSubScreen = SubScreen.Settings }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HubScreen(
    viewModel: MainViewModel,
    onNavigateToNotes: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val notes by viewModel.notes.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Espace Outils & Gestion",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        // Tuile Notes
        HubTile(
            title = "Notes & Résumés géomatiques",
            subtitle = "${notes.size} note(s) enregistrée(s)",
            icon = Icons.Default.StickyNote2,
            iconTint = VertEmeraude,
            onClick = onNavigateToNotes
        )

        // Tuile Sauvegarde Drive
        HubTile(
            title = "Sauvegarde Google Drive",
            subtitle = if (!settings.driveConnectedAccount.isNullOrBlank()) "Connecté (${settings.driveConnectedAccount})" else "Dossier GeoParcours UV-BF",
            icon = Icons.Default.CloudSync,
            iconTint = OrangeTerreCuite,
            onClick = onNavigateToBackup
        )

        // Tuile Paramètres & Profil
        HubTile(
            title = "Profil Étudiant & Paramètres",
            subtitle = "${settings.studentName} • ${settings.matricule}",
            icon = Icons.Default.Settings,
            iconTint = BrunTerre,
            onClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.weight(1f))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Université Virtuelle du Burkina Faso",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = VertEmeraude)
                )
                Text(
                    text = "Filière Géomatique • L1 (S1 & S2)",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Développé pour un usage personnel 100% autonome et hors-ligne.",
                    style = MaterialTheme.typography.labelSmall.copy(color = GrisNeutre, fontSize = 10.sp)
                )
            }
        }
    }
}

@Composable
fun HubTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GrisNeutre
            )
        }
    }
}
