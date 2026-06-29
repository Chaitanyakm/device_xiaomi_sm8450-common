/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.data

object PerfMonNative {
    init {
        System.loadLibrary("perfmon_jni")
    }

    const val DIRTY_FPS       = 1 shl 0
    const val DIRTY_CPU_USAGE = 1 shl 1
    const val DIRTY_CPU_TEMP  = 1 shl 2
    const val DIRTY_GPU_USAGE = 1 shl 3
    const val DIRTY_GPU_TEMP  = 1 shl 4
    const val DIRTY_GPU_FREQ  = 1 shl 5
    const val DIRTY_RAM       = 1 shl 6

    const val IDX_DIRTY     = 0
    const val IDX_FPS       = 1
    const val IDX_CPU_USAGE = 2
    const val IDX_CPU_TEMP  = 3
    const val IDX_GPU_USAGE = 4
    const val IDX_GPU_TEMP  = 5
    const val IDX_GPU_FREQ  = 6
    const val IDX_RAM_USED  = 7
    const val BUFFER_SIZE   = 8

    external fun initNative()
    external fun closeNative()
    external fun pollFastNative(outBuffer: IntArray)
    external fun pollSlowNative(outBuffer: IntArray)
}
