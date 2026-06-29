/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.perfmon

import androidx.compose.runtime.Immutable

@Immutable
data class PerfMonStatsSnapshot(
    val fps: String = "0",
    val cpuUsage: String = "0%",
    val cpuTemp: String = "0°C",
    val gpuUsage: String = "0%",
    val gpuTemp: String = "0°C",
    val gpuFreq: String = "0MHz",
    val ramUsage: String = "0MB"
)
