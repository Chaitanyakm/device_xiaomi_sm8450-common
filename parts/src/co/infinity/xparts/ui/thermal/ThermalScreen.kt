/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.ui.thermal

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.infinity.xparts.R
import co.infinity.xparts.data.ThermalUtils.ThermalState
import co.infinity.xparts.ui.components.AppIcon
import co.infinity.xparts.ui.components.MainSwitchBar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalScreen(
    viewModel: ThermalViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppThermalState?>(null) }
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val view = LocalView.current
    val context = LocalContext.current
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.thermal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            showResetDialog = true 
                        },
                        enabled = uiState.isEnabled && uiState.apps.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.thermal_reset))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            MainSwitchBar(
                title = stringResource(R.string.thermal_enable),
                summary = stringResource(R.string.thermal_summary),
                checked = uiState.isEnabled,
                onCheckedChange = { viewModel.toggleThermalEnabled(it) }
            )
            
            AnimatedVisibility(
                visible = uiState.isEnabled,
                enter = slideInVertically(
                    initialOffsetY = { -it }, 
                    animationSpec = spring(stiffness = 1200f, dampingRatio = 0.6f)
                ) + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(stiffness = 1200f, dampingRatio = 0.6f)
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.apps.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.thermal_apps_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    when {
                        uiState.isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        uiState.error != null -> ErrorState(error = uiState.error!!)
                        uiState.apps.isEmpty() -> EmptyState()
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(
                                    items = uiState.apps,
                                    key = { it.packageName }
                                ) { app ->
                                    AppThermalItem(
                                        app = app,
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            selectedApp = app
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (selectedApp != null) {
        val currentApp = selectedApp!!
        
        ModalBottomSheet(
            onDismissRequest = { selectedApp = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ThermalProfileGridSheet(
                currentApp = currentApp,
                onProfileSelected = { newState ->
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    
                    val profileName = context.getString(newState.label)
                    Toast.makeText(context, "Applied $profileName to ${currentApp.label}", Toast.LENGTH_SHORT).show()
                    viewModel.updateAppThermalState(currentApp.packageName, newState)
                    selectedApp = null 
                }
            )
        }
    }

    if (showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                viewModel.resetProfiles()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
fun AppThermalItem(
    app: AppThermalState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(drawable = app.icon, contentDescription = app.label, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant 
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(app.currentState.label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ThermalProfileGridSheet(
    currentApp: AppThermalState,
    onProfileSelected: (ThermalState) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val sheetHeight = screenHeight * 0.35f 

    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val transition = updateTransition(transitionState, label = "SheetEnter")
    
    val offsetY by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy) },
        label = "OffsetY"
    ) { visible -> if (visible) 0.dp else 250.dp }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200) },
        label = "Alpha"
    ) { visible -> if (visible) 1f else 0f }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .offset(y = offsetY)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(drawable = currentApp.icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = currentApp.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = stringResource(R.string.thermal_title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = sheetHeight)
        ) {
            items(ThermalState.values()) { state ->
                ThermalGridItem(
                    state = state,
                    isSelected = state == currentApp.currentState,
                    onItemClick = onProfileSelected
                )
            }
        }
    }
}

@Composable
fun ThermalGridItem(
    state: ThermalState,
    isSelected: Boolean,
    onItemClick: (ThermalState) -> Unit
) {
    var isClicked by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isClicked) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )

    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { isClicked = true }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = getIconForState(state), contentDescription = null, tint = contentColor, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(state.label),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }

    LaunchedEffect(isClicked) {
        if (isClicked) {
            delay(100)
            onItemClick(state)
        }
    }
}

private fun getIconForState(state: ThermalState): ImageVector {
    return when (state) {
        ThermalState.BENCHMARK -> Icons.Rounded.Speed
        ThermalState.BROWSER -> Icons.Rounded.Public
        ThermalState.CAMERA -> Icons.Rounded.CameraAlt
        ThermalState.DIALER -> Icons.Rounded.Phone
        ThermalState.GAMING -> Icons.Rounded.SportsEsports
        ThermalState.NAVIGATION -> Icons.Rounded.NearMe
        ThermalState.VIDEOCALL -> Icons.Rounded.VideoCall
        ThermalState.VIDEO -> Icons.Rounded.PlayCircle
        ThermalState.DEFAULT -> Icons.Rounded.Android
    }
}

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.thermal_reset)) },
        text = { Text(text = stringResource(R.string.thermal_reset_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = "Yes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "No") } }
    )
}

@Composable
private fun ErrorState(error: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Error: $error",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(text = "No apps found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Install some apps to manage thermal profiles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
