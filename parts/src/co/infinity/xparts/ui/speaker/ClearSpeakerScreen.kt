/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.speaker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.infinity.xparts.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearSpeakerScreen(
    viewModel: ClearSpeakerViewModel,
    onBackPressed: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.clear_speaker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    SpeakerWaveAnimation()
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-32).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.clear_speaker_summary),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.clear_speaker_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                FilledTonalButton(
                    onClick = { viewModel.toggleCleaning(!isPlaying) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = if (isPlaying) "Stop Cleaning" else "Start Cleaning",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                if (isPlaying) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cleaning in progress... Turn volume to max.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SpeakerWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveParams")
    val color = MaterialTheme.colorScheme.primary
    
    val wave1 by animateWave(infiniteTransition, 0)
    val wave2 by animateWave(infiniteTransition, 600)
    val wave3 by animateWave(infiniteTransition, 1200)

    Canvas(modifier = Modifier.size(300.dp)) {
        val center = this.center
        val baseRadius = 60.dp.toPx()
        val maxRadius = size.minDimension / 2
        
        fun drawWave(progress: Float) {
            val currentRadius = baseRadius + ((maxRadius - baseRadius) * progress)
            val alpha = (1f - progress) * 0.5f
            
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
        }

        drawWave(wave1)
        drawWave(wave2)
        drawWave(wave3)
    }
}

@Composable
fun animateWave(transition: InfiniteTransition, delay: Int): State<Float> {
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delay, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveProgress"
    )
}
