package com.ired.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.model.RemoteType
import com.ired.app.model.UserRemote
import com.ired.app.ui.theme.AccentBlue
import com.ired.app.ui.theme.AccentRed
import com.ired.app.ui.theme.DarkBackground
import com.ired.app.ui.theme.DarkSurface
import com.ired.app.ui.theme.DarkSurfaceVariant
import com.ired.app.ui.theme.Shape6
import com.ired.app.ui.theme.ShapeCircle
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import com.ired.app.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val remotes by viewModel.remotes.collectAsState()
    val selectedRemoteId by viewModel.selectedRemoteId.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var remoteToDelete by remember { mutableStateOf<UserRemote?>(null) }

    // Instant toast feedback with zero layout displacement
    val context = LocalContext.current
    var currentToast by remember { mutableStateOf<Toast?>(null) }

    LaunchedEffect(Unit) {
        viewModel.statusEvents.collect { event ->
            currentToast?.cancel()
            val toast = Toast.makeText(context, event.message, Toast.LENGTH_SHORT)
            currentToast = toast
            toast.show()
        }
    }

    val tabIds = remember(remotes) { remotes.map { it.id } + listOf("settings") }
    val selectedIndex = tabIds.indexOf(selectedRemoteId).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Minimalist Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IRED REMOTE",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Hardware IR Blaster • Oppo Find X9 Ultra",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(ShapeCircle)
                        .background(AccentBlue)
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Remote",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Device Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = DarkSurface,
            contentColor = AccentBlue,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = AccentBlue
                    )
                }
            }
        ) {
            remotes.forEach { remote ->
                val isSelected = remote.id == selectedRemoteId
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.selectRemote(remote.id) },
                    text = {
                        Text(
                            text = remote.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) TextPrimary else TextMuted
                        )
                    }
                )
            }

            val isSettingsSelected = selectedRemoteId == "settings"
            Tab(
                selected = isSettingsSelected,
                onClick = { viewModel.selectRemote("settings") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isSettingsSelected) TextPrimary else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Settings",
                            fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSettingsSelected) TextPrimary else TextMuted,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            )

            // Quick Add Tab
            Tab(
                selected = false,
                onClick = { showAddDialog = true },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(Shape6)
                            .background(AccentBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Add",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AccentBlue,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            )
        }

        // Active Remote Screen with buttery-smooth crossfade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Crossfade(
                targetState = selectedRemoteId,
                animationSpec = tween(durationMillis = 180),
                label = "screen_transition"
            ) { targetId ->
                if (targetId == "settings") {
                    SettingsScreen(viewModel = viewModel)
                } else {
                    val currentRemote = remotes.find { it.id == targetId }
                    if (currentRemote != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Sub-header with Category and Remove action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${currentRemote.type.category} • ${currentRemote.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(Shape6)
                                        .clickable { remoteToDelete = currentRemote }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = AccentRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Remove",
                                        fontSize = 11.sp,
                                        color = AccentRed.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }

                            // Dedicated Device Screen
                            Box(modifier = Modifier.weight(1f)) {
                                when (currentRemote.type) {
                                    RemoteType.LG_TV -> LgTvScreen(remote = currentRemote, viewModel = viewModel)
                                    RemoteType.GOOGLE_TV -> GoogleTvScreen(remote = currentRemote, viewModel = viewModel)
                                    RemoteType.PANASONIC_AC -> PanasonicAcScreen(remote = currentRemote, viewModel = viewModel)
                                    RemoteType.CHERRY_LIGHTS -> CherryLightsScreen(remote = currentRemote, viewModel = viewModel)
                                    RemoteType.CUSTOM_REMOTE -> CustomRemoteScreen(remote = currentRemote, viewModel = viewModel)
                                }
                            }
                        }
                    } else {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRemoteDialog(
            onDismiss = { showAddDialog = false },
            onRemoteAdded = { newRemote ->
                viewModel.addRemote(newRemote)
                showAddDialog = false
            }
        )
    }

    remoteToDelete?.let { remote ->
        AlertDialog(
            onDismissRequest = { remoteToDelete = null },
            title = { Text("Remove '${remote.name}'?", color = TextPrimary) },
            text = { Text("This remote will be removed from your active remotes list. You can add it back anytime.", color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeRemote(remote.id)
                        remoteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { remoteToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }
}
