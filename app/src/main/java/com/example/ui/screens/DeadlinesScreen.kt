package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseModule
import com.example.data.model.Deadline
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class DeadlineViewMode {
    LIST,
    CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val deadlines by viewModel.deadlines.collectAsState()
    val modules by viewModel.modules.collectAsState()

    var viewMode by remember { mutableStateOf(DeadlineViewMode.LIST) }
    var showCompleted by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Pour le calendrier
    var calendarMonthOffset by remember { mutableIntStateOf(0) }
    var selectedCalendarDay by remember { mutableStateOf<Calendar?>(null) }

    val filteredDeadlines = remember(deadlines, showCompleted, selectedCalendarDay, viewMode) {
        if (viewMode == DeadlineViewMode.CALENDAR && selectedCalendarDay != null) {
            val sel = selectedCalendarDay!!
            deadlines.filter { dl ->
                val dlCal = Calendar.getInstance().apply { timeInMillis = dl.dueDate }
                dlCal.get(Calendar.YEAR) == sel.get(Calendar.YEAR) &&
                dlCal.get(Calendar.DAY_OF_YEAR) == sel.get(Calendar.DAY_OF_YEAR)
            }
        } else {
            deadlines.filter { if (showCompleted) true else !it.isCompleted }
                .sortedBy { it.dueDate }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = OrangeTerreCuite,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_deadline_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter une échéance")
            }
        },
        modifier = modifier.testTag("deadlines_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barre de basculement Liste / Calendrier
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = viewMode == DeadlineViewMode.LIST,
                        onClick = { viewMode = DeadlineViewMode.LIST },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Vue Liste")
                    }
                    SegmentedButton(
                        selected = viewMode == DeadlineViewMode.CALENDAR,
                        onClick = { viewMode = DeadlineViewMode.CALENDAR },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Vue Calendrier")
                    }
                }

                if (viewMode == DeadlineViewMode.LIST) {
                    FilterChip(
                        selected = showCompleted,
                        onClick = { showCompleted = !showCompleted },
                        label = { Text("Terminées", fontSize = 12.sp) }
                    )
                }
            }

            if (viewMode == DeadlineViewMode.CALENDAR) {
                // Vue calendrier mensuel
                CalendarMonthlyView(
                    monthOffset = calendarMonthOffset,
                    onPrevMonth = { calendarMonthOffset -= 1 },
                    onNextMonth = { calendarMonthOffset += 1 },
                    deadlines = deadlines,
                    selectedDay = selectedCalendarDay,
                    onSelectDay = { selectedCalendarDay = it }
                )
            }

            // Liste des échéances
            if (filteredDeadlines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = VertEmeraude,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (viewMode == DeadlineViewMode.CALENDAR) "Aucune échéance pour ce jour." else "Aucune échéance à afficher.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDeadlines, key = { it.id }) { dl ->
                        val mod = modules.find { it.id == dl.moduleId }
                        DeadlineCardItem(
                            deadline = dl,
                            module = mod,
                            onToggle = { viewModel.toggleDeadline(dl) },
                            onDelete = { viewModel.deleteDeadline(dl) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDeadlineDialog(
            modules = modules,
            onDismiss = { showAddDialog = false },
            onConfirm = { deadline ->
                viewModel.saveDeadline(deadline)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CalendarMonthlyView(
    monthOffset: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    deadlines: List<Deadline>,
    selectedDay: Calendar?,
    onSelectDay: (Calendar?) -> Unit
) {
    val cal = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.FRENCH) }
    val monthTitle = monthYearFormat.format(cal.time).replaceFirstChar { it.uppercase() }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7 // 0 = Lundi

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header mois + flèches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Mois précédent")
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onNextMonth) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Mois suivant")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Jours de la semaine (Lun..Dim)
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekDays = listOf("L", "M", "M", "J", "V", "S", "D")
                weekDays.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grille des jours
            val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - firstDayOfWeek + 1

                        if (dayNum in 1..daysInMonth) {
                            val dayCal = Calendar.getInstance().apply {
                                timeInMillis = cal.timeInMillis
                                set(Calendar.DAY_OF_MONTH, dayNum)
                            }
                            val isToday = isSameDay(dayCal, Calendar.getInstance())
                            val isSelected = selectedDay != null && isSameDay(dayCal, selectedDay)

                            // Has deadlines ?
                            val countDeadlines = deadlines.count { dl ->
                                val dlCal = Calendar.getInstance().apply { timeInMillis = dl.dueDate }
                                isSameDay(dayCal, dlCal)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> OrangeTerreCuite
                                            isToday -> VertEmeraudeContainerLight
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        if (isSelected) onSelectDay(null) else onSelectDay(dayCal)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> VertEmeraude
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    )
                                    if (countDeadlines > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else OrangeTerreCuite)
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun DeadlineCardItem(
    deadline: Deadline,
    module: CourseModule?,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isOverdue = deadline.dueDate < now && !deadline.isCompleted
    val diffHours = ((deadline.dueDate - now) / (1000 * 60 * 60))
    val isUrgent = diffHours in 0..48 && !deadline.isCompleted

    val dateFormat = remember { SimpleDateFormat("EEEE dd MMMM à HH:mm", Locale.FRENCH) }
    val dateText = dateFormat.format(Date(deadline.dueDate)).replaceFirstChar { it.uppercase() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                deadline.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                isOverdue -> StatutEnRetard.copy(alpha = 0.08f)
                isUrgent -> OrangeContainerLight.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (deadline.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Statut",
                        tint = if (deadline.isCompleted) VertEmeraude else GrisNeutre
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = when (deadline.type) {
                                "Examen" -> StatutEnRetard.copy(alpha = 0.15f)
                                "Devoir" -> OrangeContainerLight
                                "Forum" -> VertEmeraudeContainerLight
                                else -> BeigeSable
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = deadline.type,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when (deadline.type) {
                                        "Examen" -> StatutEnRetard
                                        "Devoir" -> OrangeTerreCuite
                                        "Forum" -> VertEmeraude
                                        else -> BrunTerre
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (module != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = module.code,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = deadline.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (deadline.isCompleted) GrisNeutre else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (deadline.description.isNotBlank()) {
                        Text(
                            text = deadline.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isOverdue || isUrgent) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                deadline.isCompleted -> GrisNeutre
                                isOverdue -> StatutEnRetard
                                isUrgent -> OrangeTerreCuite
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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

@Composable
fun AddDeadlineDialog(
    modules: List<CourseModule>,
    onDismiss: () -> Unit,
    onConfirm: (Deadline) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Devoir") }
    var selectedModuleId by remember { mutableStateOf<Long?>(modules.firstOrNull()?.id) }
    var reminderEnabled by remember { mutableStateOf(true) }

    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
    }
    var dueDateMs by remember { mutableLongStateOf(calendar.timeInMillis) }

    val context = LocalContext.current
    val displayDateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH) }

    val types = listOf("Devoir", "Examen", "Forum", "Réinscription", "Autre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une échéance", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'échéance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type selector
                Text("Type :", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.take(3).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.drop(3).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                // Date & Time picker
                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = dueDateMs }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                c.set(Calendar.YEAR, y)
                                c.set(Calendar.MONTH, m)
                                c.set(Calendar.DAY_OF_MONTH, d)
                                TimePickerDialog(
                                    context,
                                    { _, hour, min ->
                                        c.set(Calendar.HOUR_OF_DAY, hour)
                                        c.set(Calendar.MINUTE, min)
                                        dueDateMs = c.timeInMillis
                                    },
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Échéance : ${displayDateFormat.format(Date(dueDateMs))}")
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Consignes / Détails optionnels") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            Deadline(
                                title = title.trim(),
                                description = description.trim(),
                                dueDate = dueDateMs,
                                type = type,
                                moduleId = selectedModuleId,
                                reminderEnabled = reminderEnabled
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeTerreCuite)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
