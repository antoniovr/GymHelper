package com.tibarra.gymhelper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tibarra.gymhelper.ui.theme.DarkBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GymButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
fun GymCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun IncrementalPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 3600,
    step: Int = 1,
    unit: String = "",
    modifier: Modifier = Modifier,
    formatter: ((Int) -> String)? = null
) {
    val scope = rememberCoroutineScope()
    val currentValue = rememberUpdatedState(value)
    val onValueChangeState = rememberUpdatedState(onValueChange)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .pointerInput(min, step) {
                    detectTapGestures(
                        onPress = {
                            var isLongPress = false
                            val job = scope.launch {
                                delay(400)
                                isLongPress = true
                                while (currentValue.value > min) {
                                    onValueChangeState.value((currentValue.value - step).coerceAtLeast(min))
                                    delay(60)
                                }
                            }
                            try {
                                awaitRelease()
                            } finally {
                                job.cancel()
                                if (!isLongPress && currentValue.value > min) {
                                    onValueChangeState.value(currentValue.value - step)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Less", tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatter?.invoke(value) ?: value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (unit.isNotBlank() && formatter == null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Increment Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .pointerInput(max, step) {
                    detectTapGestures(
                        onPress = {
                            var isLongPress = false
                            val job = scope.launch {
                                delay(400)
                                isLongPress = true
                                while (currentValue.value < max) {
                                    onValueChangeState.value((currentValue.value + step).coerceAtMost(max))
                                    delay(60)
                                }
                            }
                            try {
                                awaitRelease()
                            } finally {
                                job.cancel()
                                if (!isLongPress && currentValue.value < max) {
                                    onValueChangeState.value(currentValue.value + step)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "More", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EffortRatingBar(
    selectedRating: Int,
    onRatingSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        (1..5).forEach { rating ->
            val isSelected = selectedRating == rating
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f), CircleShape)
                    .clickable { onRatingSelected(rating) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rating.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) DarkBackground else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
