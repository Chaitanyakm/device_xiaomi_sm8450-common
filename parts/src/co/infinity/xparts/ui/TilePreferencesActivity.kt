/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import co.infinity.xparts.ui.perfmon.PerfMonTileService
import co.infinity.xparts.ui.perfmon.PerfMonComposeActivity

class TilePreferencesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentName: ComponentName? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        }

        val className = componentName?.className

        val targetClass: Class<*>? = when (className) {
            PerfMonTileService::class.java.name -> PerfMonComposeActivity::class.java
            else -> null
        }

        if (targetClass != null) {
            val newIntent = Intent(this, targetClass)
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(newIntent)
        }

        finish()
    }
}
