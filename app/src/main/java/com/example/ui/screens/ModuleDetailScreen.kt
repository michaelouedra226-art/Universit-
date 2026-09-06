package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseModule
import com.example.data.model.ResourceLink
import com.example.data.model.SubTask
import com.example.ui.MainViewModel
import com.example.ui.components.AcademicCircularProgress
import com.example.ui.components.ModuleStatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStartTimer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val modules by viewModel.modules.collectAsState()
    val module = modules.find { it.id == moduleId }

    if (module == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Module introuvable.")
        }
        return
    }

    val subTasks by viewModel.getSubTasksForModule(moduleId).collectAsState(initial = emptyList())
    val resources by viewModel.getResourcesForModule(moduleId).collectAsState(initial = emptyList())
    val allSessions by viewModel.sessions.collectAsState()
    val moduleSessions = remember(allSessions, moduleId) {
        allSessions.filter { it.moduleId == moduleId }
    }

    val context = LocalContext.current
    var notesText by remember(module.notes) { mutableStateOf(module.notes) }
    var newSubTaskTitle by remember { mutableStateOf("") }
    var showAddResourceDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = module.code, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { onStartTimer(module.id) }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Chrono pour ce module",
                            tint = OrangeTerreCuite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("module_detail_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // En-tête titre + crédits + statut
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Semestre ${module.semester} • ${module.credits} Crédits ECTS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            ModuleStatusBadge(
                                progression = module.progression,
                                targetEndDate = module.targetEndDate
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Progression & Contrôles rapides
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AcademicCircularProgress(
                            progressPercent = module.progression,
                            sizeDp = 120.dp,
                            strokeWidth = 12.dp,
                            label = "Progression"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            value = module.progression.toFloat(),
                            onValueChange = { newProg ->
                                viewModel.updateModuleProgression(module.id, newProg.toInt())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = VertEmeraude,
                                activeTrackColor = VertEmeraude
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Boutons rapides d'incrément (+5%, +10%, +25%, 100%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(5, 10, 25).forEach { step ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateModuleProgression(
                                            module.id,
                                            (module.progression + step).coerceAtMost(100)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+$step%")
                                }
                            }
                            Button(
                                onClick = { viewModel.updateModuleProgression(module.id, 100) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatutTermine),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("100%", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Dates prévues (Début et Fin)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Planning & Échéance prévue",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Date de début
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Date de début",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                val startStr = if (module.startDate > 0) dateFormat.format(Date(module.startDate)) else "Non définie"
                                TextButton(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                cal.set(y, m, d, 0, 0, 0)
                                                viewModel.saveModule(module.copy(startDate = cal.timeInMillis))
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                ) {
                                    Text(startStr, fontWeight = FontWeight.Bold, color = VertEmeraude)
                                }
                            }

                            // Date de fin
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Date de fin prévue",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                val endStr = if (module.targetEndDate > 0) dateFormat.format(Date(module.targetEndDate)) else "Non définie"
                                TextButton(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                cal.set(y, m, d, 23, 59, 59)
                                                viewModel.saveModule(module.copy(targetEndDate = cal.timeInMillis))
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                ) {
                                    Text(endStr, fontWeight = FontWeight.Bold, color = OrangeTerreCuite)
                                }
                            }
                        }
                    }
                }
            }

            // Sous-tâches (Checklist)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sous-tâches & TD/TPs (${subTasks.count { it.isCompleted }}/${subTasks.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Input ajout rapide sous-tâche
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubTaskTitle,
                                onValueChange = { newSubTaskTitle = it },
                                placeholder = { Text("Ajouter un chapitre, exercice...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newSubTaskTitle.isNotBlank()) {
                                        viewModel.addSubTask(module.id, newSubTaskTitle)
                                        newSubTaskTitle = ""
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VertEmeraude)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Ajouter",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (subTasks.isEmpty()) {
                            Text(
                                text = "Aucune sous-tâche ajoutée pour le moment.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            subTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { viewModel.toggleSubTask(task) },
                                        colors = CheckboxDefaults.colors(checkedColor = VertEmeraude)
                                    )
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (task.isCompleted) GrisNeutre else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteSubTask(task) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Supprimer",
                                            tint = GrisNeutre,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ressources / Liens
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ressources & Liens (${resources.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(onClick = { showAddResourceDialog = true }) {
                                Text("+ Ajouter", color = VertEmeraude, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (resources.isEmpty()) {
                            Text(
                                text = "Aucun lien ou support de cours ajouté.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                resources.forEach { res ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = OrangeTerreCuite,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = res.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                if (res.urlOrPath.isNotBlank()) {
                                                    Text(
                                                        text = res.urlOrPath,
                                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteResource(res) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Supprimer",
                                                    tint = GrisNeutre,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Zone de notes personnelles
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notes personnelles de cours",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            TextButton(
                                onClick = {
                                    viewModel.updateModuleNotes(module.id, notesText)
                                }
                            ) {
                                Text("Enregistrer", color = VertEmeraude, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("Écrivez vos résumés, formules géodésiques, commandes QGIS...") },
                            minLines = 4,
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Historique des sessions d'étude liées
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Historique d'étude (${moduleSessions.size} sessions)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (moduleSessions.isEmpty()) {
                            Text(
                                text = "Aucune session d'étude enregistrée pour ce module. Utilisez le chronomètre pour suivre votre temps.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        } else {
                            val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                moduleSessions.take(5).forEach { sess ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = timeFormat.format(Date(sess.timestamp)),
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Text(
                                            text = "${sess.durationSeconds / 60} min (${sess.mode})",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = VertEmeraude)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bouton de suppression si module personnalisé
            if (module.isCustom) {
                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteModule(module)
                            onBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatutEnRetard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supprimer ce module personnalisé")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showAddResourceDialog) {
        var resTitle by remember { mutableStateOf("") }
        var resUrl by remember { mutableStateOf("") }
        var resType by remember { mutableStateOf("cours") }

        AlertDialog(
            onDismissRequest = { showAddResourceDialog = false },
            title = { Text("Ajouter une ressource", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = resTitle,
                        onValueChange = { resTitle = it },
                        label = { Text("Titre de la ressource") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = resUrl,
                        onValueChange = { resUrl = it },
                        label = { Text("Lien ou référence (URL, Moodle...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resTitle.isNotBlank()) {
                            viewModel.addResource(module.id, resTitle, resUrl, resType)
                            showAddResourceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude)
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddResourceDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
