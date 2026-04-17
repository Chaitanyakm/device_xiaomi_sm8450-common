/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.data

import android.content.Context
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import androidx.preference.PreferenceManager
import co.infinity.xparts.utils.Logging

class SaturationUtils private constructor(private val context: Context) {

    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private var surfaceFlinger: IBinder? = null

    init {
        try {
            surfaceFlinger = ServiceManager.getService("SurfaceFlinger")
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to get SurfaceFlinger service", e)
        }
    }

    /**
     * Restore saturation from settings with a delay (to ensure boot finishes).
     */
    fun restoreSaturation() {
        Handler(Looper.getMainLooper()).postDelayed({
            val savedValue = getSavedSaturation()
            Logging.d(TAG, "Restoring saturation: $savedValue")
            setSaturation(savedValue)
        }, 5000)
    }

    fun getSavedSaturation(): Int {
        return sharedPrefs.getInt(KEY_SATURATION, DEFAULT_SATURATION)
    }

    fun setSaturation(value: Int) {
        if (sharedPrefs.getInt(KEY_SATURATION, DEFAULT_SATURATION) != value) {
            sharedPrefs.edit().putInt(KEY_SATURATION, value).apply()
        }

        val saturationFloat = if (value == 100) 1.001f else value / 100.0f
        applyToSurfaceFlinger(saturationFloat)
    }

    private fun applyToSurfaceFlinger(saturation: Float) {
        val flinger = surfaceFlinger ?: ServiceManager.getService("SurfaceFlinger").also { surfaceFlinger = it }
        
        if (flinger == null) {
            Logging.e(TAG, "SurfaceFlinger is null, cannot apply saturation")
            return
        }

        try {
            val data = Parcel.obtain()
            data.writeInterfaceToken("android.ui.ISurfaceComposer")
            data.writeFloat(saturation)
            flinger.transact(TRANSACTION_SATURATION, data, null, 0)
            data.recycle()
            Logging.d(TAG, "Applied saturation: $saturation")
        } catch (e: RemoteException) {
            Logging.e(TAG, "Failed to apply saturation transaction", e)
        }
    }

    companion object {
        private const val TAG = "SaturationUtils"
        private const val KEY_SATURATION = "saturation"
        private const val DEFAULT_SATURATION = 100
        private const val TRANSACTION_SATURATION = 1022

        @Volatile
        private var instance: SaturationUtils? = null

        fun getInstance(context: Context): SaturationUtils {
            return instance ?: synchronized(this) {
                instance ?: SaturationUtils(context.applicationContext).also { instance = it }
            }
        }
    }
}
