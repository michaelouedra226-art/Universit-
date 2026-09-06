package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseModule
import com.example.ui.MainViewModel
import com.example.ui.components.ModuleStatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val modules by viewModel.modules.collectAsState()

    var selectedSemesterFilter by remember { mutableIntStateOf(0) } // 0 = Tous, 1 = S1, 2 = S2
    var selectedStatusFilter by remember { mutableStateOf("TOUS") } // TOUS, EN_COURS, NON_COMMENCE, TERMINE, EN_RETARD
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredModules = remember(modules, selectedSemesterFilter, selectedStatusFilter) {
        val now = System.currentTimeMillis()
        modules.filter { mod ->
            val matchSemester = when (selectedSemesterFilter) {
                1 -> mod.semester == 1
                2 -> mod.semester == 2
                else -> true
            }
            val matchStatus = when (selectedStatusFilter) {
                "NON_COMMENCE" -> mod.progression == 0
                "EN_COURS" -> mod.progression in 1..99
                "TERMINE" -> mod.progression >= 100
                "EN_RETARD" -> mod.targetEndDate in 1..<now && mod.progression < 100
                else -> true
            }
            matchSemester && matchStatus
        }
    }

    Scaffold(
        modifier = modifier.testTag("modules_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VertEmeraude,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_module_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter un module")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filtres par semestre
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSemesterFilter == 0,
                    onClick = { selectedSemesterFilter = 0 },
                    label = { Text("Tous les semestres") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VertEmeraude,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedSemesterFilter == 1,
                    onClick = { selectedSemesterFilter = 1 },
                    label = { Text("Semestre 1") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VertEmeraude,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedSemesterFilter == 2,
                    onClick = { selectedSemesterFilter = 2 },
                    label = { Text("Semestre 2") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VertEmeraude,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // Filtres secondaires par statut
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf(
                    "TOUS" to "Tous statuts",
                    "EN_COURS" to "En cours",
                    "NON_COMMENCE" to "Non commencé",
                    "TERMINE" to "Terminé",
                    "EN_RETARD" to "En retard"
                )
                items(statuses) { (key, label) ->
                    AssistChip(
                        onClick = { selectedStatusFilter = key },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedStatusFilter == key) BeigeSable else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = if (selectedStatusFilter == key) BrunTerre else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Nombre de modules affichés
            Text(
                text = "${filteredModules.size} module(s) • L1 Géomatique UV-BF",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (filteredModules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun module ne correspond aux filtres.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredModules, key = { it.id }) { module ->
                        ModuleListItem(
                            module = module,
                            onClick = { onNavigateToDetail(module.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddModuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code, name, semester, credits ->
                viewModel.saveModule(
                    CourseModule(
                        code = code,
                        name = name,
                        semester = semester,
                        credits = credits,
                        isCustom = true
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ModuleListItem(
    module: CourseModule,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "S${module.semester} • ${module.credits} Crédits",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                ModuleStatusBadge(
                    progression = module.progression,
                    targetEndDate = module.targetEndDate
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = module.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { module.progression / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (module.progression >= 100) StatutTermine else VertEmeraude,
                    trackColor = GrisClair
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${module.progression}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (module.progression >= 100) StatutTermine else VertEmeraude
                    )
                )
            }
        }
    }
}

@Composable
fun AddModuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String, semester: Int, credits: Int) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var semester by remember { mutableIntStateOf(1) }
    var creditsText by remember { mutableStateOf("4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Ajouter un module personnalisé", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Code (ex: GEO130)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Intitulé du module") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = semester.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v in 1..2) semester = v ?: 1
                        },
                        label = { Text("Semestre (1 ou 2)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = creditsText,
                        onValueChange = { creditsText = it },
                        label = { Text("Crédits (ECTS)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        val c = creditsText.toIntOrNull() ?: 4
                        onConfirm(code, name, semester, c)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude)
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
