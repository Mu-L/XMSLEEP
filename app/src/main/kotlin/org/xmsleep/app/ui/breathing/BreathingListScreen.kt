package org.xmsleep.app.ui.breathing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.xmsleep.app.R

@Composable
fun BreathingListScreen(
    onMethodClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val methods = remember { BreathingMethods.getMethods(context) }
    val pagerState = rememberPagerState(pageCount = { methods.size })
    var showTutorialMethod by remember { mutableStateOf<BreathingMethod?>(null) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = context.getString(R.string.breathing_exercise),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(440.dp),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                BreathingMethodCard(
                    method = methods[page],
                    onClick = { onMethodClick(methods[page].id) },
                    onHelpClick = { showTutorialMethod = methods[page] }
                )
            }
        }
    }

    showTutorialMethod?.let { method ->
        BreathingMethodTutorialDialog(method = method, onDismiss = { showTutorialMethod = null })
    }
}

@Composable
private fun BreathingMethodCard(
    method: BreathingMethod,
    onClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    val context = LocalContext.current
    val rhythmColor = when (method.id) {
        "sleep_478" -> Color(0xFF5C6BC0)
        "box_4444" -> Color(0xFF26A69A)
        "belly_46" -> Color(0xFF66BB6A)
        "stress_426" -> Color(0xFFFF8A65)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(rhythmColor, rhythmColor.copy(alpha = 0.85f))))
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                if (method.isPrimary) {
                    Surface(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.breathing_method_primary),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = method.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = method.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            RhythmVisualization(
                inhale = method.inhale, hold = method.hold,
                exhale = method.exhale, holdAfter = method.holdAfter, color = Color.White
            )

            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    method.tags.take(2).forEach { tag ->
                        Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(text = tag, style = MaterialTheme.typography.labelSmall, color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    IconButton(onClick = onHelpClick, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = context.getString(R.string.tutorial),
                            tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (method.inhale > 0) RhythmParam(label = context.getString(R.string.inhale).removeSuffix("..."), value = "${method.inhale}s")
                    if (method.hold > 0) RhythmParam(label = context.getString(R.string.hold).removeSuffix("..."), value = "${method.hold}s")
                    if (method.exhale > 0) RhythmParam(label = context.getString(R.string.exhale).removeSuffix("..."), value = "${method.exhale}s")
                    if (method.holdAfter > 0) RhythmParam(label = context.getString(R.string.hold_after).removeSuffix("..."), value = "${method.holdAfter}s")
                }
            }
        }
    }
}

@Composable
private fun RhythmVisualization(inhale: Int, hold: Int, exhale: Int, holdAfter: Int, color: Color) {
    val context = LocalContext.current
    val total = (inhale + hold + exhale + holdAfter).toFloat()
    if (total == 0f) return
    val segments = listOf(
        Triple(inhale, context.getString(R.string.inhale).removeSuffix("..."), 0.8f),
        Triple(hold, context.getString(R.string.hold).removeSuffix("..."), 1f),
        Triple(exhale, context.getString(R.string.exhale).removeSuffix("..."), 0.5f),
        Triple(holdAfter, context.getString(R.string.hold_after).removeSuffix("..."), 1f)
    ).filter { it.first > 0 }
    Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        segments.forEach { (seconds, label, heightRatio) ->
            Box(
                modifier = Modifier.weight(seconds / total).fillMaxHeight(heightRatio)
                    .align(Alignment.CenterVertically).clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$label${seconds}s", style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.9f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RhythmParam(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun BreathingMethodTutorialDialog(method: BreathingMethod, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
        title = { Text(method.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(method.subtitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Divider()
                Text(method.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider()
                Text(context.getString(R.string.operation_steps), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                method.steps.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Divider()
                Text(context.getString(R.string.breathing_rhythm), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(context.getString(R.string.breathing_rhythm_detail, method.inhale, method.hold, method.exhale, method.holdAfter),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.got_it)) } }
    )
}
