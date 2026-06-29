/*
 * Copyright (C) 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Display.HdrCapabilities
import co.infinity.xparts.data.AuxCameraUtils
import co.infinity.xparts.data.FpGestureUtils
import co.infinity.xparts.data.PerfMonUtils
import co.infinity.xparts.data.ThermalUtils
import co.infinity.xparts.utils.Logging

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logging.i(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> handleLockedBootCompleted(context)
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
        }
    }

    private fun handleLockedBootCompleted(context: Context) {
        Logging.i(TAG, "Handling locked boot completed.")
        try {
            // 1. Initialize Services
            startServices(context)

            // 2. Override HDR Types
            overrideHdrTypes(context)
            
            // 3. Airplane Mode
            Handler(Looper.getMainLooper()).postDelayed({
            toggleAirplaneMode(context)
            }, 5000)

        } catch (e: Exception) {
            Logging.e(TAG, "Error during locked boot completed processing", e)
        }
    }

    private fun handleBootCompleted(context: Context) {
        Logging.i(TAG, "Handling boot completed.")
        // Add additional boot-completed actions if needed
    }

    private fun startServices(context: Context) {
        Logging.i(TAG, "Starting services...")
	
        // Aux Camera Services
        AuxCameraUtils.restore(context)

        // Thermal Services
        ThermalUtils.getInstance(context).startService()

        // Performance Monitor Services
        PerfMonUtils.getInstance(context).startServiceIfEnabled()

        // Fingerprint Gestures
        if (FpGestureUtils.isAvailable(context) && FpGestureUtils.isEnabled(context)) {
            Logging.d(TAG, "Enabling Fingerprint Gesture on boot")
            FpGestureUtils.updateDriver(true)
        }
    }

    private fun overrideHdrTypes(context: Context) {
        try {
            val dm = context.getSystemService(DisplayManager::class.java)
            if (dm != null) {
                dm.overrideHdrTypes(
                    Display.DEFAULT_DISPLAY, 
                    intArrayOf(
                        HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                        HdrCapabilities.HDR_TYPE_HDR10,
                        HdrCapabilities.HDR_TYPE_HLG,
                        HdrCapabilities.HDR_TYPE_HDR10_PLUS
                    )
                )
                Logging.i(TAG, "HDR types overridden successfully.")
            }
        } catch (e: Exception) {
            Logging.e(TAG, "Error overriding HDR types", e)
        }
    }

private fun toggleAirplaneMode(context: Context) {
    Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
    context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", true))
    Handler(Looper.getMainLooper()).postDelayed({
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        context.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", false))
    }, 500)
}
    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
