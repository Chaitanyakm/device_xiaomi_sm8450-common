/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.gestures

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.infinity.xparts.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FpGestureScreen(
    viewModel: FpGestureViewModel,
    onBackPressed: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isEnabled by viewModel.isEnabled.collectAsState()
    val selectedAction by viewModel.selectedAction.collectAsState()
    val view = LocalView.current

    val boxColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val actions = listOf(
        1 to R.string.action_screenshot,
        2 to R.string.action_assistant,
        3 to R.string.action_play_pause,
        4 to R.string.action_notifications,
        5 to R.string.action_camera,
        6 to R.string.action_flashlight,
        7 to R.string.action_mute,
        8 to R.string.action_vibrate,
        9 to R.string.action_volume,
        10 to R.string.action_sleep
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { }, 
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Double-tap on fingerprint sensor",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SideFpBallAnimation(active = true)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = boxColor
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.fp_double_tap_enable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use double tap on fingerprint sensor to trigger actions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { 
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                viewModel.toggleEnabled(it) 
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isEnabled) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isEnabled) {
                items(actions.size) { index ->
                    val (id, labelRes) = actions[index]
                    val isSelected = (selectedAction == id)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else 
                                boxColor
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    viewModel.setAction(id)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { 
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    viewModel.setAction(id) 
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Padding to align with the "option starting" text (approx 16dp inside the card)
                            .padding(start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.Start // Left Aligned
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.fp_double_tap_footer),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SideFpBallAnimation(active: Boolean) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val ballColor = MaterialTheme.colorScheme.primary
    val rippleColor = MaterialTheme.colorScheme.primary
    
    val infiniteTransition = rememberInfiniteTransition(label = "BallAnim")
    
    val animState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.0f at 0 using LinearEasing
                
                1.0f at 150 using FastOutSlowInEasing 
                
                0.0f at 300 using FastOutSlowInEasing 
                
                1.0f at 450 using FastOutSlowInEasing 
                
                0.0f at 800 using FastOutSlowInEasing 
                0.0f at 2000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "BallState"
    )

    Canvas(modifier = Modifier.size(160.dp, 200.dp)) {
        scale(scale = 1.15f, pivot = center) {
            
            val phoneW = size.width * 0.5f
            val phoneH = size.height * 0.85f
            val startX = (size.width - phoneW) / 2
            val startY = (size.height - phoneH) / 2
            val rightEdgeX = startX + phoneW
            
            val outlineStroke = Stroke(width = 2.5.dp.toPx()) 

            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(startX, startY),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = outlineStroke
            )

            val volHeight = 30.dp.toPx() 

            val powerHeight = volHeight * 0.42f 
            
            val buttonX = rightEdgeX + (2.5.dp.toPx() / 2)
            
            val volY = startY + (phoneH * 0.15f)
            val gap = 12.dp.toPx() 
            val powerY = volY + volHeight + gap 
            
            drawLine(
                color = outlineColor,
                start = Offset(buttonX, volY),
                end = Offset(buttonX, volY + volHeight),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            drawLine(
                color = outlineColor,
                start = Offset(buttonX, powerY),
                end = Offset(buttonX, powerY + powerHeight),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            if (active) {
                val ballRadius = 7.2.dp.toPx()
                val ballStartX = rightEdgeX + 22.dp.toPx()
                val tapDistance = 22.dp.toPx() 
                
                val currentBallX = ballStartX - (tapDistance * animState)
                val targetY = powerY + (powerHeight / 2) 
                
                if (animState > 0.8f) {
                    val rippleRadius = (ballRadius * 0.5f) + (ballRadius * 3.5f * (animState - 0.8f) * 5f)
                    val rippleAlpha = (1f - animState) * 5f 
                    
                    drawCircle(
                        color = rippleColor.copy(alpha = rippleAlpha.coerceIn(0f, 1f)),
                        radius = rippleRadius,
                        center = Offset(rightEdgeX, targetY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                
                drawCircle(
                    color = ballColor,
                    radius = ballRadius,
                    center = Offset(currentBallX, targetY)
                )
            }
        }
    }
}
