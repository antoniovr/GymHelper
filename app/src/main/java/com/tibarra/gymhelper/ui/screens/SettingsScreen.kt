package com.tibarra.gymhelper.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tibarra.gymhelper.data.GymDatabase
import com.tibarra.gymhelper.ui.components.GymCard
import com.tibarra.gymhelper.ui.theme.*
import com.tibarra.gymhelper.ui.viewmodel.SessionViewModel
import com.tibarra.gymhelper.ui.viewmodel.WorkoutViewModel
import com.tibarra.gymhelper.util.CsvManager
import com.tibarra.gymhelper.util.PreferencesManager
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeChange: (Int) -> Unit, 
    onAccentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val db = remember { GymDatabase.getDatabase(context) }
    
    val workoutViewModel: WorkoutViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return WorkoutViewModel(db.gymDao()) as T
            }
        },
    )
    
    val sessionViewModel: SessionViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SessionViewModel(db.gymDao()) as T
            }
        },
    )

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefsManager = remember { PreferencesManager(context) }
    var countdownAudioEnabled by remember { mutableStateOf(prefsManager.isCountdownAudioEnabled) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val datePrefix = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) }

    val importMimeTypes = arrayOf(
        "text/csv", 
        "text/comma-separated-values", 
        "application/vnd.ms-excel", 
        "text/plain",
        "application/octet-stream",
    )

    // --- Settings Launchers ---
    val exportSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val csv = CsvManager.exportSettings(prefsManager)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(csv)
                    }
                }
                snackbarHostState.showSnackbar("Settings exported successfully")
            }
        }
    }

    val importSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { isr ->
                InputStreamReader(isr).use { reader ->
                    when (val result = CsvManager.importSettings(reader.readText())) {
                        is CsvManager.ValidationResult.Success -> {
                            val data = result.data
                            prefsManager.isCountdownAudioEnabled = data.countdownAudioEnabled
                            prefsManager.themeMode = data.themeMode
                            prefsManager.accentColorIndex = data.accentColorIndex
                            countdownAudioEnabled = data.countdownAudioEnabled
                            onThemeChange(data.themeMode)
                            onAccentChange(data.accentColorIndex)
                            scope.launch { snackbarHostState.showSnackbar("Settings imported successfully") }
                        }
                        is CsvManager.ValidationResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                }
            }
        }
    }

    // --- Workout Launchers ---
    val exportWorkoutsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val data = workoutViewModel.getWorkoutsBackupData()
                val csv = CsvManager.exportWorkouts(data)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(csv)
                    }
                }
                snackbarHostState.showSnackbar("Workouts exported successfully")
            }
        }
    }

    val importWorkoutsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { isr ->
                InputStreamReader(isr).use { reader ->
                    workoutViewModel.importWorkoutsFromCsv(reader.readText()) { error ->
                        scope.launch {
                            if (error == null) {
                                snackbarHostState.showSnackbar("Workouts imported successfully")
                            } else {
                                errorMessage = error
                            }
                        }
                    }
                }
            }
        }
    }

    // --- History Launchers ---
    val exportHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                val data = sessionViewModel.getHistoryBackupData()
                val csv = CsvManager.exportHistory(data)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    OutputStreamWriter(os).use { writer ->
                        writer.write(csv)
                    }
                }
                snackbarHostState.showSnackbar("History exported successfully")
            }
        }
    }

    val importHistoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { isr ->
                InputStreamReader(isr).use { reader ->
                    sessionViewModel.importHistoryFromCsv(reader.readText()) { error ->
                        scope.launch {
                            if (error == null) {
                                snackbarHostState.showSnackbar("History imported successfully")
                            } else {
                                errorMessage = error
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            .add(WindowInsets(bottom = bottomPadding)),
        topBar = {
            TopAppBar(title = { Text("SETTINGS", fontWeight = FontWeight.Bold) })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Preferences", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            item {
                GymCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Countdown Audio", fontWeight = FontWeight.Bold)
                            Text("Play beeps during the last 10 seconds of rest (only via headphones to avoid disturbing others).", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = countdownAudioEnabled,
                            onCheckedChange = {
                                countdownAudioEnabled = it
                                prefsManager.isCountdownAudioEnabled = it
                            },
                            thumbContent = if (countdownAudioEnabled) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            } else null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Theme Mode", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val options = listOf("System", "Light", "Dark")
                    val currentMode = prefsManager.themeMode
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEachIndexed { index, label ->
                            val isSelected = currentMode == index
                            Button(
                                onClick = { onThemeChange(index) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Accent Color", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
                    val accents = if (isLight) {
                        listOf(BlueStrong, GreenStrong, PurpleStrong, OrangeStrong, PinkStrong)
                    } else {
                        listOf(BluePastel, GreenPastel, PurplePastel, OrangePastel, PinkPastel)
                    }
                    val currentAccent = prefsManager.accentColorIndex

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accents.forEachIndexed { index, color ->
                            val isSelected = currentAccent == index
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onAccentChange(index) }
                            )
                        }
                    }
                }
            }

            item {
                Text("Backup & Recovery", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            item {
                GymCard {
                    Text("APP SETTINGS (PREFERENCES)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Export or import your audio preferences, theme mode and accent color.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportSettingsLauncher.launch("${datePrefix}_Settings_GH.csv") },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FileUpload, null)
                            Spacer(Modifier.width(4.dp))
                            Text("EXPORT")
                        }
                        OutlinedButton(
                            onClick = { importSettingsLauncher.launch(importMimeTypes) },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null)
                            Spacer(Modifier.width(4.dp))
                            Text("IMPORT")
                        }
                    }
                }
            }

            item {
                GymCard {
                    Text("WORKOUTS (SESSIONS CONFIG)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Export or import your workout routines and exercise configurations.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportWorkoutsLauncher.launch("${datePrefix}_Workouts_GH.csv") },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FileUpload, null)
                            Spacer(Modifier.width(4.dp))
                            Text("EXPORT")
                        }
                        OutlinedButton(
                            onClick = { importWorkoutsLauncher.launch(importMimeTypes) },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null)
                            Spacer(Modifier.width(4.dp))
                            Text("IMPORT")
                        }
                    }
                }
            }

            item {
                GymCard {
                    Text("SESSION HISTORY LOGS", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Export or import your completed training logs and statistics.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportHistoryLauncher.launch("${datePrefix}_History_GH.csv") },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FileUpload, null)
                            Spacer(Modifier.width(4.dp))
                            Text("EXPORT")
                        }
                        OutlinedButton(
                            onClick = { importHistoryLauncher.launch(importMimeTypes) },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null)
                            Spacer(Modifier.width(4.dp))
                            Text("IMPORT")
                        }
                    }
                }
            }
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Import Error") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            }
        )
    }
}
