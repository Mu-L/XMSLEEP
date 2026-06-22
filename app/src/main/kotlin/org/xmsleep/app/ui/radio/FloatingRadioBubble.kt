package org.xmsleep.app.ui.radio

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.xmsleep.app.R
import org.xmsleep.app.audio.BilibiliApi
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.ui.viewmodel.RadioViewModel
import kotlin.math.roundToInt

@Composable
fun FloatingRadioBubble(
    visible: Boolean,
    room: BilibiliApi.LiveRoom?,
    roomId: String?,
    isPlaying: Boolean,
    viewModel: RadioViewModel,
) {
    if (!visible || roomId == null) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val collapsedWidth = 32.dp
    val expandedWidth = 200.dp
    val arrowWidth = 40.dp
    val buttonHeight = 80.dp

    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isOnLeft by remember {
        mutableStateOf(PreferencesManager.getRadioFloatingButtonIsLeft(context))
    }
    var offsetX by remember {
        mutableFloatStateOf(if (isOnLeft) 0f else with(density) { screenWidth.toPx() })
    }
    var offsetY by remember {
        val savedY = PreferencesManager.getRadioFloatingButtonY(context)
        mutableFloatStateOf(
            if (savedY < 0f) {
                with(density) { (screenHeight / 2 - buttonHeight / 2).toPx() }
            } else {
                savedY
            }
        )
    }

    val width by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "width"
    )

    val height by animateDpAsState(
        targetValue = buttonHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "height"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animatedOffsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animatedOffsetY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    val buttonContainerColor = MaterialTheme.colorScheme.primaryContainer
    val buttonContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val isDarkTheme = isSystemInDarkTheme()
    val expandedContainerColor = if (isDarkTheme) {
        buttonContainerColor.copy(
            red = (buttonContainerColor.red * 1.15f).coerceAtMost(1f),
            green = (buttonContainerColor.green * 1.15f).coerceAtMost(1f),
            blue = (buttonContainerColor.blue * 1.15f).coerceAtMost(1f)
        )
    } else {
        buttonContainerColor
    }

    val buttonHeightPx = with(density) { buttonHeight.toPx() }
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val minY = 0f
    val maxY = screenHeightPx - buttonHeightPx - with(density) { 50.dp.toPx() }
    val clampedY = offsetY.coerceIn(minY, maxY)

    Box(
        modifier = Modifier.fillMaxSize().zIndex(100f)
    ) {
        if (isExpanded && room != null) {
            Row(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            if (isOnLeft) {
                                animatedOffsetX.roundToInt()
                            } else {
                                (animatedOffsetX - with(density) { (expandedWidth + arrowWidth).toPx() }).roundToInt()
                            },
                            clampedY.roundToInt()
                        )
                    }
                    .height(height),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOnLeft) {
                    ExpandedRadioContent(
                        expandedWidth = expandedWidth,
                        expandedContainerColor = expandedContainerColor,
                        expandedContentColor = buttonContentColor,
                        room = room,
                        isOnLeft = true
                    )
                    ArrowButton(
                        arrowWidth = arrowWidth,
                        buttonHeight = buttonHeight,
                        buttonContainerColor = buttonContainerColor,
                        buttonContentColor = buttonContentColor,
                        isOnLeft = true,
                        onClick = { isExpanded = false }
                    )
                } else {
                    ArrowButton(
                        arrowWidth = arrowWidth,
                        buttonHeight = buttonHeight,
                        buttonContainerColor = buttonContainerColor,
                        buttonContentColor = buttonContentColor,
                        isOnLeft = false,
                        onClick = { isExpanded = false }
                    )
                    ExpandedRadioContent(
                        expandedWidth = expandedWidth,
                        expandedContainerColor = expandedContainerColor,
                        expandedContentColor = buttonContentColor,
                        room = room,
                        isOnLeft = false
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            if (isOnLeft) {
                                animatedOffsetX.roundToInt()
                            } else {
                                (animatedOffsetX - with(density) { collapsedWidth.toPx() }).roundToInt()
                            },
                            animatedOffsetY.roundToInt()
                        )
                    }
                    .scale(if (isDragging) 1.1f else breatheScale)
                    .alpha(if (isDragging) 1f else 0.40f)
                    .width(width)
                    .height(height)
                    .then(
                        if (isDragging) {
                            Modifier.shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        color = buttonContainerColor,
                        shape = if (isDragging) {
                            RoundedCornerShape(8.dp)
                        } else {
                            RoundedCornerShape(
                                topStart = if (isOnLeft) 0.dp else 20.dp,
                                bottomStart = if (isOnLeft) 0.dp else 20.dp,
                                topEnd = if (isOnLeft) 20.dp else 0.dp,
                                bottomEnd = if (isOnLeft) 20.dp else 0.dp
                            )
                        }
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            },
                            onDragEnd = {
                                isDragging = false
                                val centerX = offsetX + with(density) { collapsedWidth.toPx() } / 2
                                val shouldBeOnLeft = centerX < screenWidthPx / 2
                                isOnLeft = shouldBeOnLeft
                                offsetX = if (shouldBeOnLeft) 0f else screenWidthPx
                                offsetY = offsetY.coerceIn(minY, maxY)
                                PreferencesManager.saveRadioFloatingButtonPosition(context, offsetY, isOnLeft)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!isDragging && room != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isExpanded = true
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                CustomChevronIcon(
                    isOnLeft = isOnLeft,
                    color = buttonContentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomChevronIcon(
    isOnLeft: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            if (isOnLeft) {
                moveTo(size.width * 0.3f, size.height * 0.2f)
                lineTo(size.width * 0.7f, size.height * 0.5f)
                lineTo(size.width * 0.3f, size.height * 0.8f)
            } else {
                moveTo(size.width * 0.7f, size.height * 0.2f)
                lineTo(size.width * 0.3f, size.height * 0.5f)
                lineTo(size.width * 0.7f, size.height * 0.8f)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun ArrowButton(
    arrowWidth: Dp,
    buttonHeight: Dp,
    buttonContainerColor: Color,
    buttonContentColor: Color,
    isOnLeft: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(arrowWidth)
            .height(buttonHeight)
            .background(
                color = buttonContainerColor,
                shape = RoundedCornerShape(
                    topStart = if (isOnLeft) 0.dp else 20.dp,
                    bottomStart = if (isOnLeft) 0.dp else 20.dp,
                    topEnd = if (isOnLeft) 20.dp else 0.dp,
                    bottomEnd = if (isOnLeft) 20.dp else 0.dp
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isOnLeft) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = buttonContentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ExpandedRadioContent(
    expandedWidth: Dp,
    expandedContainerColor: Color,
    expandedContentColor: Color,
    room: BilibiliApi.LiveRoom,
    isOnLeft: Boolean
) {
    Box(
        modifier = Modifier
            .width(expandedWidth)
            .fillMaxHeight()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(0.dp)
            )
            .background(
                color = expandedContainerColor,
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(
                text = room.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = expandedContentColor,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = room.userName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = expandedContentColor.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
