package com.tibarra.gymhelper.wear.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.tibarra.gymhelper.wear.util.CommandSender

@Composable
fun WearApp(state: SessionUiState) {
    val context = LocalContext.current
    var wasResting by remember { mutableStateOf(false) }

    // Vibration when rest ends
    LaunchedEffect(state.isResting) {
        if (wasResting && !state.isResting) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
            vibrator.vibrate(effect)
        }
        wasResting = state.isResting
    }

    if (state.isResting) {
        RestTimerScreen(state)
    } else if (!state.isStarted) {
        IdleScreen()
    } else {
        SessionScreen(state)
    }
}

@Composable
fun IdleScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Waiting for mobile session...",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SessionScreen(state: SessionUiState) {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp, start = 10.dp, end = 10.dp)
    ) {
        // Workout Title
        item {
            Text(
                text = state.workoutName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }

        // Warm-up Button
        item {
            val color = if (state.isWarmupActive) Color.Yellow else Color.DarkGray
            val label = if (state.isWarmupActive) "STOP WARMUP" else "START WARMUP"
            
            CompactButton(
                onClick = {
                    val cmd = if (state.isWarmupActive) SyncUtils.CMD_STOP_WARMUP else SyncUtils.CMD_START_WARMUP
                    CommandSender.send(context, cmd)
                },
                colors = ButtonDefaults.buttonColors(containerColor = color),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
            }
        }

        // Exercises
        itemsIndexed(state.exercises) { exIndex, ex ->
            ActiveExerciseCard(ex) { setIndex ->
                CommandSender.send(context, SyncUtils.CMD_TOGGLE_SET, "$exIndex,$setIndex")
            }
        }

        // Cardio Button
        item {
            val color = if (state.isCardioActive) Color(0xFFFFB300) else Color.DarkGray
            val label = if (state.isCardioActive) "STOP CARDIO" else "START CARDIO"
            
            CompactButton(
                onClick = {
                    val cmd = if (state.isCardioActive) SyncUtils.CMD_STOP_CARDIO else SyncUtils.CMD_START_CARDIO
                    CommandSender.send(context, cmd)
                },
                colors = ButtonDefaults.buttonColors(containerColor = color),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun ActiveExerciseCard(ex: ExerciseState, onToggleSet: (Int) -> Unit) {
    Card(
        onClick = { },
        enabled = ex.isInteractionAllowed,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ex.isCompleted) Color.Gray.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Text(
                text = "${ex.index}. ${ex.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (ex.variantNotes.isNotBlank()) {
                Text(
                    text = ex.variantNotes,
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // BIG SET BUTTONS - Using a simple Row to avoid experimental FlowRow issues
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ex.sets.take(4).forEachIndexed { sIndex, s ->
                    BigSetBubble(s, ex.isInteractionAllowed) { onToggleSet(sIndex) }
                }
            }
            if (ex.sets.size > 4) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    ex.sets.drop(4).forEachIndexed { sIndex, s ->
                        BigSetBubble(s, ex.isInteractionAllowed) { onToggleSet(sIndex + 4) }
                    }
                }
            }
        }
    }
}

@Composable
fun BigSetBubble(s: SetState, enabled: Boolean, onClick: () -> Unit) {
    val isPending = !s.isCompleted
    val color = when {
        s.isCompleted -> MaterialTheme.colorScheme.primary
        enabled -> Color.Gray.copy(alpha = 0.3f)
        else -> Color.Gray.copy(alpha = 0.1f)
    }
    
    val contentColor = if (s.isCompleted) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(34.dp)
            .background(color, CircleShape)
            .then(if (enabled && isPending) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (s.isDropSet) "D" else s.setNumber.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = contentColor
        )
    }
}

@Composable
fun RestTimerScreen(state: SessionUiState) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "RESTING",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(state.restTimeLeft),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Red,
                fontSize = 44.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { CommandSender.send(context, SyncUtils.CMD_SKIP_REST) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
            ) {
                Text("SKIP", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
