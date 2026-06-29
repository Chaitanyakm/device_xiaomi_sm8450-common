/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.perfmon

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.infinity.xparts.R
import co.infinity.xparts.data.PerfMonUtils
import co.infinity.xparts.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfMonScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val utils = remember { PerfMonUtils.getInstance(context) }

    var isEnabled by remember { mutableStateOf(utils.isEnabled) }
    var alpha by remember { mutableStateOf(utils.overlayAlpha) }
    var textSize by remember { mutableStateOf(utils.textSizeSp) }
    var showCompact by remember { mutableStateOf(utils.showCompact) }
    var showFps by remember { mutableStateOf(utils.showFps) }
    var showCpuUsage by remember { mutableStateOf(utils.showCpuUsage) }
    var showCpuTemp by remember { mutableStateOf(utils.showCpuTemp) }
    var showGpuUsage by remember { mutableStateOf(utils.showGpuUsage) }
    var showGpuTemp by remember { mutableStateOf(utils.showGpuTemp) }
    var showGpuSpeed by remember { mutableStateOf(utils.showGpuSpeed) }
    var showRam by remember { mutableStateOf(utils.showRam) }
    var touchPassthrough by remember { mutableStateOf(utils.touchPassthrough) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.perfmon_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                MainSwitchBar(
                    title = stringResource(R.string.perfmon_enable),
                    summary = stringResource(R.string.perfmon_summary),
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        utils.isEnabled = it
                    }
                )
            }

            item {
                SectionHeader(stringResource(R.string.perfmon_appearance))
                SliderItem(stringResource(R.string.perfmon_bg_transparency), alpha, 0.1f..1f, enabled = true) { alpha = it; utils.overlayAlpha = it }
                SliderItem(stringResource(R.string.perfmon_text_size), textSize, 10f..24f, enabled = !showCompact) { textSize = it; utils.textSizeSp = it }
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(stringResource(R.string.perfmon_metrics))
            }

            item {
                Column {
                    SettingsToggle(stringResource(R.string.perfmon_touch_passthrough), touchPassthrough) { touchPassthrough = !touchPassthrough; utils.touchPassthrough = touchPassthrough }
                    SettingsToggle(stringResource(R.string.perfmon_compact_mode), showCompact) { showCompact = !showCompact; utils.showCompact = showCompact }
                    SettingsToggle(stringResource(R.string.perfmon_show_fps), showFps) { showFps = !showFps; utils.showFps = showFps }
                    
                    SettingsToggle(stringResource(R.string.perfmon_show_cpu), showCpuUsage) { showCpuUsage = !showCpuUsage; utils.showCpuUsage = showCpuUsage }
                    AnimatedVisibility(visible = showCpuUsage, enter = expandVertically(), exit = shrinkVertically()) {
                        Column {
                            SettingsToggle(stringResource(R.string.perfmon_show_cpu_temp), showCpuTemp) { showCpuTemp = !showCpuTemp; utils.showCpuTemp = showCpuTemp }
                        }
                    }
                    
                    SettingsToggle(stringResource(R.string.perfmon_show_gpu), showGpuUsage) { showGpuUsage = !showGpuUsage; utils.showGpuUsage = showGpuUsage }
                    AnimatedVisibility(visible = showGpuUsage, enter = expandVertically(), exit = shrinkVertically()) {
                        Column {
                            SettingsToggle(stringResource(R.string.perfmon_show_gpu_temp), showGpuTemp) { showGpuTemp = !showGpuTemp; utils.showGpuTemp = showGpuTemp }
                            if (!showCompact) {
                                SettingsToggle(stringResource(R.string.perfmon_show_gpu_speed), showGpuSpeed) { showGpuSpeed = !showGpuSpeed; utils.showGpuSpeed = showGpuSpeed }
                            }
                        }
                    }
                    
                    SettingsToggle(stringResource(R.string.perfmon_show_ram), showRam) { showRam = !showRam; utils.showRam = showRam }
                }
            }
        }
    }
}
