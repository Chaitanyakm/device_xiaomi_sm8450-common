/*
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import co.infinity.xparts.R
import co.infinity.xparts.data.PerfMonUtils
import co.infinity.xparts.ui.perfmon.PerfMonOverlayContent
import kotlinx.coroutines.launch

class PerfMonService : LifecycleService(), SavedStateRegistryOwner {

    companion object {
        private const val NOTIFICATION_ID = 9081
    }

    private lateinit var windowManager: WindowManager
    private lateinit var utils: PerfMonUtils
    private lateinit var controller: PerfMonController

    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val savedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var isDisplayAttached = true

    override fun onCreate() {
        super.onCreate()

        savedStateRegistryController.performRestore(null)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        utils = PerfMonUtils.getInstance(this)
        controller = PerfMonController(lifecycleScope)

        createNotificationChannel()
        registerDisplayReceiver()

        lifecycleScope.launch {
            utils.configFlow.collect { config ->
                updateTouchPassthrough(config.touchPassthrough)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!utils.isEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (composeView == null) {
            showOverlay()
        }

        startMonitoring()
        return START_STICKY
    }

    private fun showOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PerfMonService)
            setViewTreeSavedStateRegistryOwner(this@PerfMonService)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )

            setContent {
                PerfMonOverlayContent(
                    statsFlow = controller.statsFlow,
                    configFlow = utils.configFlow
                )
            }
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            if (utils.touchPassthrough) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 120
        }

        attachDragListener(composeView!!)
        windowManager.addView(composeView, layoutParams)
    }

    private fun updateTouchPassthrough(enabled: Boolean) {
        layoutParams?.let { params ->
            params.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                if (enabled) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0

            composeView?.let {
                windowManager.updateViewLayout(it, params)
            }
        }
    }

    private fun startMonitoring() {
        if (!isDisplayAttached) return
        controller.startMonitoring(isGaming = false)
    }

    private fun stopMonitoring() {
        controller.stopMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "perfmon_channel",
                "Performance Monitor",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        startForeground(
            NOTIFICATION_ID,
            Notification.Builder(this, "perfmon_channel")
                .setContentTitle(getString(R.string.perfmon_title))
                .setContentText("Overlay is active")
                .setSmallIcon(R.drawable.ic_perfmon_settings)
                .build()
        )
    }

    private fun registerDisplayReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(displayReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private val displayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isDisplayAttached = false
                    stopMonitoring()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isDisplayAttached = true
                    if (utils.isEnabled) startMonitoring()
                }
            }
        }
    }

    private fun attachDragListener(view: View) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams!!.x
                    startY = layoutParams!!.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = startX + (event.rawX - touchX).toInt()
                    val newY = startY + (event.rawY - touchY).toInt()

                    if (layoutParams!!.x != newX || layoutParams!!.y != newY) {
                        layoutParams!!.x = newX
                        layoutParams!!.y = newY
                        windowManager.updateViewLayout(view, layoutParams)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        stopMonitoring()
        runCatching { unregisterReceiver(displayReceiver) }

        composeView?.let {
            it.disposeComposition()
            windowManager.removeViewImmediate(it)
        }
        composeView = null
        super.onDestroy()
    }
}
