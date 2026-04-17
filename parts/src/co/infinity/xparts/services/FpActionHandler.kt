/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.services

import android.app.SearchManager
import android.app.StatusBarManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.KeyEvent
import co.infinity.xparts.data.FpGestureUtils
import co.infinity.xparts.utils.Logging
import com.android.internal.app.AssistUtils
import com.android.internal.util.ScreenshotHelper

class FpActionHandler(private val context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val statusBarManager = context.getSystemService(StatusBarManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val searchManager = context.getSystemService(SearchManager::class.java)
    
    private val handler = Handler(Looper.getMainLooper())
    private val screenshotHelper = ScreenshotHelper(context)
    
    private val vibrationEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
    private val vibrationAttrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)

    private var isTorchOn = false

    init {
        cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == "0") isTorchOn = enabled
            }
        }, handler)
    }

    fun handleEvent() {
        if (!FpGestureUtils.isEnabled(context)) return
        if (!powerManager.isInteractive) return // TODO work when screen is off

        val action = FpGestureUtils.getAction(context)
        Logging.d(TAG, "Triggering action: $action")

        when (action) {
            1 -> takeScreenshot()
            2 -> launchAssist()
            3 -> playPauseMedia()
            4 -> showNotifications()
            5 -> launchCamera()
            6 -> toggleFlashlight()
            7 -> toggleRingerMode(AudioManager.RINGER_MODE_SILENT)
            8 -> toggleRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            9 -> showVolumePanel()
            10 -> goToSleep()
        }
    }

    private fun vibrate() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(vibrationEffect, vibrationAttrs)
        }
    }

    private fun takeScreenshot() {
        screenshotHelper.takeScreenshot(1, handler, null) // 1 = SCREENSHOT_VENDOR_GESTURE
    }

    private fun launchAssist() {
        vibrate()
        val args = Bundle()
        args.putLong(Intent.EXTRA_TIME, SystemClock.uptimeMillis())
        args.putInt(AssistUtils.INVOCATION_TYPE_KEY, AssistUtils.INVOCATION_TYPE_PHYSICAL_GESTURE)
        searchManager?.launchAssist(args)
    }

    private fun playPauseMedia() {
        vibrate()
        val time = SystemClock.uptimeMillis()
        val down = KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
        audioManager?.dispatchMediaKeyEvent(down)
        val up = KeyEvent(time, time, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0)
        audioManager?.dispatchMediaKeyEvent(up)
    }

    private fun showNotifications() {
        statusBarManager?.expandNotificationsPanel()
    }

    private fun launchCamera() {
        vibrate()
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun toggleFlashlight() {
        vibrate()
        try {
            cameraManager?.setTorchMode("0", !isTorchOn)
        } catch (e: Exception) {
            Logging.e(TAG, "Error toggling flashlight", e)
        }
    }

    private fun toggleRingerMode(targetMode: Int) {
        vibrate()
        val currentMode = audioManager?.ringerModeInternal ?: AudioManager.RINGER_MODE_NORMAL
        val newMode = if (currentMode != targetMode) targetMode else AudioManager.RINGER_MODE_NORMAL
        audioManager?.ringerModeInternal = newMode
    }

    private fun showVolumePanel() {
        audioManager?.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    private fun goToSleep() {
        vibrate()
        powerManager?.goToSleep(SystemClock.uptimeMillis())
    }

    companion object {
        private const val TAG = "FpActionHandler"
    }
}
