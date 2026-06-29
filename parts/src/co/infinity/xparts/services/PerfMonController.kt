/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.services

import co.infinity.xparts.data.PerfMonNative
import co.infinity.xparts.ui.perfmon.PerfMonStatsSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PerfMonController(private val scope: CoroutineScope) {

    private val _statsFlow = MutableStateFlow(PerfMonStatsSnapshot())
    val statsFlow = _statsFlow.asStateFlow()

    private val nativeBuf = IntArray(PerfMonNative.BUFFER_SIZE)
    private var fastJob: Job? = null
    private var slowJob: Job? = null

    private var cachedFps = "0"
    private var cachedCpuUsage = "0%"
    private var cachedCpuTemp = "0°C"
    private var cachedGpuUsage = "0%"
    private var cachedGpuTemp = "0°C"
    private var cachedGpuFreq = "0MHz"
    private var cachedRam = "0MB"

    fun startMonitoring(isGaming: Boolean = false) {
        if (fastJob?.isActive == true) return
        PerfMonNative.initNative()

        val fastInterval = if (isGaming) 100L else 250L
        val slowInterval = 1000L

        fastJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                PerfMonNative.pollFastNative(nativeBuf)
                val dirty = nativeBuf[PerfMonNative.IDX_DIRTY]
                if (dirty != 0) {
                    if ((dirty and PerfMonNative.DIRTY_FPS) != 0) {
                        cachedFps = nativeBuf[PerfMonNative.IDX_FPS].toString()
                    }
                    if ((dirty and PerfMonNative.DIRTY_CPU_USAGE) != 0) {
                        cachedCpuUsage = "${nativeBuf[PerfMonNative.IDX_CPU_USAGE]}%"
                    }
                    if ((dirty and PerfMonNative.DIRTY_GPU_USAGE) != 0) {
                        cachedGpuUsage = "${nativeBuf[PerfMonNative.IDX_GPU_USAGE]}%"
                    }
                    if ((dirty and PerfMonNative.DIRTY_GPU_FREQ) != 0) {
                        cachedGpuFreq = "${nativeBuf[PerfMonNative.IDX_GPU_FREQ]}MHz"
                    }
                    emitSnapshot()
                }
                delay(fastInterval)
            }
        }

        slowJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                PerfMonNative.pollSlowNative(nativeBuf)
                val dirty = nativeBuf[PerfMonNative.IDX_DIRTY]
                if (dirty != 0) {
                    if ((dirty and PerfMonNative.DIRTY_CPU_TEMP) != 0) {
                        cachedCpuTemp = "${nativeBuf[PerfMonNative.IDX_CPU_TEMP]}°C"
                    }
                    if ((dirty and PerfMonNative.DIRTY_GPU_TEMP) != 0) {
                        cachedGpuTemp = "${nativeBuf[PerfMonNative.IDX_GPU_TEMP]}°C"
                    }
                    if ((dirty and PerfMonNative.DIRTY_RAM) != 0) {
                        cachedRam = "${nativeBuf[PerfMonNative.IDX_RAM_USED]}MB"
                    }
                    emitSnapshot()
                }
                delay(slowInterval)
            }
        }
    }

    private fun emitSnapshot() {
        val next = PerfMonStatsSnapshot(
            fps = cachedFps,
            cpuUsage = cachedCpuUsage,
            cpuTemp = cachedCpuTemp,
            gpuUsage = cachedGpuUsage,
            gpuTemp = cachedGpuTemp,
            gpuFreq = cachedGpuFreq,
            ramUsage = cachedRam
        )
        if (next != _statsFlow.value) {
            _statsFlow.value = next
        }
    }

    fun stopMonitoring() {
        fastJob?.cancel()
        slowJob?.cancel()
        fastJob = null
        slowJob = null
        PerfMonNative.closeNative()
    }
}
