/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.thermal

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import co.infinity.xparts.data.ThermalUtils
import co.infinity.xparts.theme.XPartsTheme
import co.infinity.xparts.utils.Logging

/**
 * Thermal profile settings activity using Jetpack Compose.
 */
class ThermalComposeActivity : ComponentActivity() {
    private lateinit var thermalUtils: ThermalUtils
    private lateinit var launcherApps: LauncherApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        Logging.d(TAG, "onCreate")
        
        thermalUtils = ThermalUtils.getInstance(this)
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        
        setContent {
            XPartsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ThermalViewModel = viewModel(
                        factory = ThermalViewModelFactory(
                            context = applicationContext,
                            thermalUtils = thermalUtils,
                            launcherApps = launcherApps
                        )
                    )
                    
                    ThermalScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logging.d(TAG, "onDestroy")
    }

    companion object {
        private const val TAG = "ThermalComposeActivity"
    }
}
