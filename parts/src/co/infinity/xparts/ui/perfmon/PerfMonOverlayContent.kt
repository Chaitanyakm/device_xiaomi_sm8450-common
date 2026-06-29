/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.perfmon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.infinity.xparts.data.PerfMonConfigSnapshot
import kotlinx.coroutines.flow.StateFlow

private val OverlayShape = RoundedCornerShape(10.dp)

@Composable
fun PerfMonOverlayContent(
    statsFlow: StateFlow<PerfMonStatsSnapshot>,
    configFlow: StateFlow<PerfMonConfigSnapshot>
) {
    val stats by statsFlow.collectAsStateWithLifecycle()
    val config by configFlow.collectAsStateWithLifecycle()

    val backgroundColor = remember(config.alpha) {
        Color.Black.copy(alpha = config.alpha)
    }

    Column(
        modifier = Modifier
            .background(backgroundColor, OverlayShape)
            .padding(8.dp)
    ) {
        if (config.showCompact) {
            CompactOverlay(stats, config)
        } else {
            ExpandedOverlay(stats, config)
        }
    }
}

@Composable
private fun CompactOverlay(stats: PerfMonStatsSnapshot, config: PerfMonConfigSnapshot) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (config.showFps) MiniItem("FPS", stats.fps)
        if (config.showCpuUsage) {
            val cpuDisplay = if (config.showCpuTemp) "${stats.cpuUsage} | ${stats.cpuTemp}" else stats.cpuUsage
            MiniItem("CPU", cpuDisplay)
        }
    }
    if (config.showGpuUsage || config.showRam) {
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (config.showGpuUsage) {
                val gpuDisplay = if (config.showGpuTemp) "${stats.gpuUsage} | ${stats.gpuTemp}" else stats.gpuUsage
                MiniItem("GPU", gpuDisplay)
            }
            if (config.showRam) MiniItem("RAM", stats.ramUsage)
        }
    }
}

@Composable
private fun ExpandedOverlay(stats: PerfMonStatsSnapshot, config: PerfMonConfigSnapshot) {
    val textSize = remember(config.textSize) { config.textSize.sp }
    val secondarySize = remember(config.textSize) { (config.textSize * 0.85f).sp }

    if (config.showFps) MetricRow("FPS", stats.fps, Color(0xFF4CAF50), textSize)
    if (config.showCpuUsage) {
        val cpuDisplay = if (config.showCpuTemp) "${stats.cpuUsage} | ${stats.cpuTemp}" else stats.cpuUsage
        MetricRow("CPU", cpuDisplay, Color(0xFF58A6FF), textSize)
    }
    if (config.showGpuUsage) {
        val gpuDisplay = if (config.showGpuTemp) "${stats.gpuUsage} | ${stats.gpuTemp}" else stats.gpuUsage
        MetricRow("GPU", gpuDisplay, Color(0xFFFF8626), textSize)
        if (config.showGpuSpeed) {
            Text(
                text = "speed:${stats.gpuFreq}",
                color = Color(0xFFFFB347),
                fontSize = secondarySize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    if (config.showRam) MetricRow("RAM", stats.ramUsage, Color(0xFFFFC107), textSize)
}

@Composable
private fun MiniItem(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MetricRow(label: String, value: String, color: Color, size: TextUnit) {
    Row(
        modifier = Modifier.padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = size, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text(text = value, color = color, fontSize = size, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}
