package com.tibarra.gymhelper.wear.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.tibarra.gymhelper.shared.SyncUtils
import com.tibarra.gymhelper.shared.model.ExerciseState
import com.tibarra.gymhelper.shared.model.SessionUiState
import com.tibarra.gymhelper.shared.model.SetState
import com.tibarra.gymhelper.wear.R
import com.tibarra.gymhelper.wear.util.CommandSender
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun getAccentColor(index: Int): Color {
    return when (index) {
        1 -> Color(0xFFA5D6A7) // Green Pastel
        2 -> Color(0xFFCE93D8) // Purple Pastel
        3 -> Color(0xFFFFCC80) // Orange Pastel
        4 -> Color(0xFFF48FB1) // Pink Pastel
        else -> Color(0xFF79B8FF) // Blue Pastel (Default)
    }
}

@Composable
fun WearApp(state: SessionUiState, onFinish: () -> Unit) {
    val context = LocalContext.current
    var wasResting by remember { mutableStateOf(false) }
    var selectedNotes by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(state.isResting) {
        if (wasResting && !state.isResting) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
            vibrator.vibrate(effect)
        }
        wasResting = state.isResting
    }

    val accentColor = getAccentColor(state.accentColorIndex)

    if (selectedNotes != null) {
        NotesFullScreen(
            title = selectedNotes!!.first,
            notes = selectedNotes!!.second,
            onBack = { selectedNotes = null },
            accentColor = accentColor
        )
    } else if (state.isResting) {
        RestTimerScreen(state, accentColor)
    } else if (state.isWarmupActive && state.warmupEndTimestamp > 0) {
        CountdownScreen(
            title = "WARM-UP",
            endTimestamp = state.warmupEndTimestamp,
            accentColor = accentColor,
            onStop = { CommandSender.send(context, SyncUtils.CMD_STOP_WARMUP) }
        )
    } else if (state.isCardioActive && state.cardioEndTimestamp > 0) {
        CountdownScreen(
            title = "CARDIO",
            endTimestamp = state.cardioEndTimestamp,
            accentColor = accentColor,
            isCardio = true,
            onStop = { payload -> CommandSender.send(context, SyncUtils.CMD_STOP_CARDIO, payload) }
        )
    } else if (!state.isStarted) {
        IdleScreen(onFinish, accentColor)
    } else {
        SessionScreen(
            state = state, 
            accentColor = accentColor,
            onShowNotes = { title, notes -> selectedNotes = title to notes }
        )
    }
}

@Composable
fun IdleScreen(onFinish: () -> Unit, accentColor: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gym_dumbbell),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Waiting for session...",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Correct circular button for Wear OS
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close, 
                    contentDescription = "Close App",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SessionScreen(state: SessionUiState, accentColor: Color, onShowNotes: (String, String) -> Unit) {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 44.dp, bottom = 44.dp, start = 12.dp, end = 12.dp)
    ) {
        item {
            Text(
                text = state.workoutName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }

        // Warm-up Button: Hide if timestamp is set and not active (meaning finished)
        val isWarmupFinished = !state.isWarmupActive && state.warmupEndTimestamp > 0
        if (!isWarmupFinished) {
            item {
                Button(
                    onClick = { CommandSender.send(context, SyncUtils.CMD_START_WARMUP) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("START WARMUP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        itemsIndexed(state.exercises) { exIndex, ex ->
            ActiveExerciseCard(ex, accentColor, onShowNotes) { setIndex ->
                CommandSender.send(context, SyncUtils.CMD_TOGGLE_SET, "$exIndex,$setIndex")
            }
        }

        // Cardio Button
        item {
            val isFinished = state.isCardioFinished
            val label = if (isFinished) "CARDIO FINISHED" else "START CARDIO"
            
            Button(
                onClick = { if (!isFinished) CommandSender.send(context, SyncUtils.CMD_START_CARDIO) },
                colors = ButtonDefaults.buttonColors(containerColor = if (isFinished) Color.Gray.copy(alpha = 0.3f) else Color.DarkGray),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isFinished
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ActiveExerciseCard(ex: ExerciseState, accentColor: Color, onShowNotes: (String, String) -> Unit, onToggleSet: (Int) -> Unit) {
    Card(
        onClick = { },
        enabled = ex.isInteractionAllowed,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ex.isCompleted) Color.Gray.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${ex.index}. ${ex.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (ex.variantNotes.isNotBlank()) {
                    IconButton(
                        onClick = { onShowNotes(ex.name, ex.variantNotes) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Notes", tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                }
                if (ex.isCompleted) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enforce sequential set selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ex.sets.take(4).forEachIndexed { sIndex, s ->
                    val isPreviousCompleted = sIndex == 0 || ex.sets[sIndex - 1].isCompleted
                    BigSetBubble(s, ex.isInteractionAllowed && isPreviousCompleted, accentColor) { onToggleSet(sIndex) }
                }
            }
            if (ex.sets.size > 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    ex.sets.drop(4).forEachIndexed { localIndex, s ->
                        val globalIndex = localIndex + 4
                        val isPreviousCompleted = ex.sets[globalIndex - 1].isCompleted
                        BigSetBubble(s, ex.isInteractionAllowed && isPreviousCompleted, accentColor) { onToggleSet(globalIndex) }
                    }
                }
            }
        }
    }
}

@Composable
fun BigSetBubble(s: SetState, enabled: Boolean, accentColor: Color, onClick: () -> Unit) {
    val isPending = !s.isCompleted
    val color = when {
        s.isCompleted -> accentColor
        enabled -> Color.Gray.copy(alpha = 0.4f)
        else -> Color.Gray.copy(alpha = 0.1f)
    }
    val contentColor = if (s.isCompleted) Color.Black else Color.White

    Box(
        modifier = Modifier.size(38.dp).background(color, CircleShape)
            .then(if (enabled && isPending) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (s.isDropSet) "D" else s.setNumber.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = contentColor
        )
    }
}

@Composable
fun NotesFullScreen(title: String, notes: String, onBack: () -> Unit, accentColor: Color) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 44.dp, bottom = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            item {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = accentColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text(text = notes, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Button(
                    onClick = onBack, 
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BACK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RestTimerScreen(state: SessionUiState, accentColor: Color) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(text = "RESTING", style = MaterialTheme.typography.labelMedium, color = Color.Red, fontWeight = FontWeight.Bold)
            Text(text = formatTime(state.restTimeLeft), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = Color.Red, fontSize = 44.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { CommandSender.send(context, SyncUtils.CMD_SKIP_REST) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth(0.85f).height(48.dp)
            ) {
                Text("SKIP", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
fun CountdownScreen(
    title: String, 
    endTimestamp: Long, 
    accentColor: Color, 
    isCardio: Boolean = false, 
    onStop: (String) -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(500.milliseconds)
        }
    }

    val remainingTotal = ((endTimestamp - currentTime) / 1000).toInt()
    val isExtra = remainingTotal < 0
    val displaySeconds = if (isExtra) -remainingTotal else remainingTotal
    val modeLabel = if (isExtra) "EXTRA $title" else title

    Box(modifier = Modifier.fillMaxSize().background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(
                text = modeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(displaySeconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 44.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isCardio) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(0.9f)) {
                    Button(
                        onClick = { onStop("pause") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("PAUSE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onStop("finish") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("FINISH", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            } else {
                Button(
                    onClick = { onStop("") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth(0.85f).height(48.dp)
                ) {
                    Text("STOP", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
