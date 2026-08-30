package com.tibarra.gymhelper.ui.previews

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tibarra.gymhelper.ui.components.GymButton
import com.tibarra.gymhelper.ui.components.GymCard
import com.tibarra.gymhelper.ui.theme.GymHelperTheme
import com.tibarra.gymhelper.ui.theme.DarkBackground
import com.tibarra.gymhelper.ui.theme.BluePastel

@Preview(showBackground = true, backgroundColor = 0xFF1A2634, widthDp = 360, heightDp = 640)
@Composable
fun ActiveSessionPreview() {
    GymHelperTheme {
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FULL BODY", style = MaterialTheme.typography.headlineSmall, color = BluePastel)
                    Text("12:45", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                GymCard {
                    Text("Bench Press", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Dumbbells", color = BluePastel)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SetMockButton(isCompleted = true, label = "S1", modifier = Modifier.weight(1f))
                        SetMockButton(isCompleted = false, label = "S2", modifier = Modifier.weight(1f))
                        SetMockButton(isCompleted = false, label = "S3", modifier = Modifier.weight(1f))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                GymCard {
                    Text("CARDIO: Treadmill", style = MaterialTheme.typography.titleLarge)
                    Text("05:00", style = MaterialTheme.typography.displayMedium, color = BluePastel, modifier = Modifier.align(Alignment.CenterHorizontally))
                    GymButton(text = "Start Cardio", onClick = {})
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A2634, widthDp = 360, heightDp = 640)
@Composable
fun RestTimerPreview() {
    GymHelperTheme {
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            Column(
                modifier = Modifier.fillMaxSize().background(DarkBackground.copy(alpha = 0.98f)).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("RESTING", style = MaterialTheme.typography.headlineLarge, color = BluePastel)
                Text("45", style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = {}) { Text("-10s") }
                    OutlinedButton(onClick = {}) { Text("+10s") }
                }
                Spacer(modifier = Modifier.height(48.dp))
                GymButton(text = "Skip Rest", onClick = {}, containerColor = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SetMockButton(isCompleted: Boolean, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(if (isCompleted) BluePastel else Color.DarkGray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground)
        else Text(label, fontWeight = FontWeight.Bold)
    }
}
