package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BackupVersion
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val backupVersions by viewModel.backupVersions.collectAsState()
    val context = LocalContext.current

    var isBackingUp by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedRestoreVersion by remember { mutableStateOf<BackupVersion?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val isDriveConnected = !settings.driveConnectedAccount.isNullOrBlank()

    Scaffold(
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = Color.White)
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(msg)
                }
            }
        },
        modifier = modifier.testTag("backup_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // État de connexion Google Drive
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDriveConnected) VertEmeraudeContainerLight else MaterialTheme.colorScheme.surface
                    ),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isDriveConnected) VertEmeraude else GrisNeutre.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (isDriveConnected) Color.White else GrisNeutre
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isDriveConnected) "Google Drive Connecté" else "Google Drive Non Connecté",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isDriveConnected) settings.driveConnectedAccount ?: "" else "Dossier : GeoParcours UV-BF",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            if (isDriveConnected) {
                                TextButton(onClick = { viewModel.disconnectGoogleDrive() }) {
                                    Text("Déconnecter", color = StatutEnRetard)
                                }
                            } else {
                                Button(
                                    onClick = { showConnectDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude)
                                ) {
                                    Text("Connecter")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dernier statut
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = VertEmeraude,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Statut : ${settings.lastBackupStatus}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }

            // Action : Sauvegarder maintenant
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
                        Text(
                            text = "Sauvegarde en 1 clic",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Exporte l'intégralité des modules, échéances, notes et sessions d'étude au format JSON horodaté.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                isBackingUp = true
                                viewModel.performBackup { success, message ->
                                    isBackingUp = false
                                    snackbarMessage = message
                                }
                            },
                            enabled = !isBackingUp,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeTerreCuite),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sauvegarde en cours...")
                            } else {
                                Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Créer une sauvegarde maintenant")
                            }
                        }
                    }
                }
            }

            // 5 Dernières Versions Sauvegardées
            item {
                Text(
                    text = "Historique des sauvegardes (5 versions conservées)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (backupVersions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Aucune version enregistrée pour l'instant.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            } else {
                items(backupVersions) { version ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = version.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${version.dateString} • ${(version.sizeBytes / 1024) + 1} Ko",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("GeoParcours Backup", version.jsonContent)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "JSON copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copier", tint = VertEmeraude)
                                }

                                Button(
                                    onClick = { selectedRestoreVersion = version },
                                    colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Restaurer", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Outils avancés (Import / Restauration manuelle de JSON)
            item {
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importer / Restaurer un fichier JSON externe")
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Dialogue de confirmation de restauration
    selectedRestoreVersion?.let { ver ->
        AlertDialog(
            onDismissRequest = { selectedRestoreVersion = null },
            title = { Text("Restaurer cette version ?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Attention : la restauration de la version du ${ver.dateString} remplacera toutes les données actuelles de l'application par celles du fichier de sauvegarde."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(ver) { success ->
                            selectedRestoreVersion = null
                            snackbarMessage = if (success) "Restauration terminée avec succès." else "Erreur lors de la restauration."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatutEnRetard)
                ) {
                    Text("Confirmer la restauration")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRestoreVersion = null }) { Text("Annuler") }
            }
        )
    }

    // Dialogue de connexion Google
    if (showConnectDialog) {
        var emailInput by remember { mutableStateOf(settings.studentName.lowercase().replace(" ", "") + "@uv.bf") }
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("Connexion Google Drive UV-BF", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "L'application synchronisera automatiquement les sauvegardes dans le dossier sécurisé « GeoParcours UV-BF » sur votre Google Drive.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Adresse email Google / UV-BF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            viewModel.connectGoogleDrive(emailInput.trim())
                            showConnectDialog = false
                            snackbarMessage = "Connecté à Google Drive : $emailInput"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude)
                ) {
                    Text("Autoriser et Connecter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) { Text("Annuler") }
            }
        )
    }

    // Dialogue d'import de JSON
    if (showImportDialog) {
        var rawJsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Coller le JSON de sauvegarde", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Collez ci-dessous le contenu JSON exporté d'une sauvegarde précédente :",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = rawJsonInput,
                        onValueChange = { rawJsonInput = it },
                        label = { Text("JSON de sauvegarde") },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawJsonInput.isNotBlank()) {
                            viewModel.restoreFromJsonText(rawJsonInput) { success ->
                                showImportDialog = false
                                snackbarMessage = if (success) "Données restaurées avec succès." else "Erreur : format JSON invalide."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VertEmeraude)
                ) {
                    Text("Restaurer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Annuler") }
            }
        )
    }
}
