/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.auxcamera

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
import co.infinity.xparts.data.AuxCameraUtils
import co.infinity.xparts.theme.XPartsTheme

class AuxCameraActivity : ComponentActivity() {

    private lateinit var launcherApps: LauncherApps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        setContent {
            XPartsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AuxCameraViewModel = viewModel(
                        factory = AuxCameraViewModelFactory(
                            applicationContext,
                            launcherApps,
                            AuxCameraUtils
                        )
                    )

                    AuxCameraScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}
