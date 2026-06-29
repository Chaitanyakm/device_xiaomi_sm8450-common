/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Paranoid Android
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.auxcamera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.infinity.xparts.R
import co.infinity.xparts.ui.components.AppIcon
import co.infinity.xparts.ui.components.MainSwitchBar
import co.infinity.xparts.ui.components.StyledSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuxCameraScreen(
    viewModel: AuxCameraViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aux_camera_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            MainSwitchBar(
                title = stringResource(R.string.aux_camera_enable),
                summary = stringResource(R.string.aux_camera_summary),
                checked = uiState.isEnabled,
                onCheckedChange = { viewModel.toggleEnabled(it) }
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.apps) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(enabled = uiState.isEnabled) {
                                viewModel.toggleApp(app.packageName, !app.enabled)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(drawable = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = app.label)
                            }
                            StyledSwitch(
                                checked = app.enabled,
                                onToggle = { viewModel.toggleApp(app.packageName, it) },
                                enabled = uiState.isEnabled
                            )
                        }
                    }
                }
            }
        }
    }
}
