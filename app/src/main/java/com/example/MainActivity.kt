package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.ApkDropViewModel
import com.example.ui.dialogs.ApkDetailDialog
import com.example.ui.dialogs.AppUpdateDialog
import com.example.ui.dialogs.DirectUrlDialog
import com.example.ui.dialogs.ExtractAppsDialog
import com.example.ui.dialogs.ParseErrorGuideDialog
import com.example.ui.screens.ApkVaultScreen
import com.example.ui.screens.BeamToTvScreen
import com.example.ui.screens.DownloadsCatalogScreen
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.TvGuideScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeLight
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.MazzeCyan
import com.example.ui.theme.MazzePurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.tvFocusHighlight

class MainActivity : ComponentActivity() {

    private val viewModel: ApkDropViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to enable edge-to-edge", e)
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: ApkDropViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val catalogApks by viewModel.catalogApks.collectAsStateWithLifecycle()
    val vaultApks by viewModel.vaultApks.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    val showExtractDialog by viewModel.showExtractDialog.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()

    val showDirectUrlDialog by viewModel.showDirectUrlDialog.collectAsStateWithLifecycle()
    val showParseErrorDialog by viewModel.showParseErrorDialog.collectAsStateWithLifecycle()
    val selectedApkDetail by viewModel.selectedApkDetail.collectAsStateWithLifecycle()

    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Android / TV remote back handler: close dialogs first, then go back to Downloads tab
    val hasOpenDialog = updateInfo != null || showExtractDialog || showDirectUrlDialog || showParseErrorDialog || selectedApkDetail != null
    BackHandler(enabled = hasOpenDialog) {
        when {
            updateInfo != null -> viewModel.dismissUpdateDialog()
            showExtractDialog -> viewModel.setShowExtractDialog(false)
            showDirectUrlDialog -> viewModel.setShowDirectUrlDialog(false)
            showParseErrorDialog -> viewModel.setShowParseErrorDialog(false)
            selectedApkDetail != null -> viewModel.openApkDetail(null)
        }
    }

    BackHandler(enabled = !hasOpenDialog && currentTab != AppTab.DOWNLOADS) {
        viewModel.setTab(AppTab.DOWNLOADS)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBarHeader(
                serverRunning = serverStatus.isRunning,
                serverIp = serverStatus.ipAddress,
                serverPort = serverStatus.port,
                isCheckingUpdate = isCheckingUpdate,
                onAddUrlClick = { viewModel.setShowDirectUrlDialog(true) },
                onHelpClick = { viewModel.setShowParseErrorDialog(true) },
                onCheckUpdateClick = { viewModel.checkForUpdates(isManual = true) }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        floatingActionButton = {
            if (currentTab == AppTab.DOWNLOADS || currentTab == AppTab.VAULT) {
                FloatingActionButton(
                    onClick = { viewModel.loadInstalledApps() },
                    containerColor = FireOrange,
                    contentColor = Color.Black,
                    modifier = Modifier
                        .tvFocusHighlight(shape = RoundedCornerShape(16.dp))
                        .testTag("fab_extract_app")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = "Extract App")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DOWNLOADS -> DownloadsCatalogScreen(
                    viewModel = viewModel,
                    catalogApks = catalogApks
                )
                AppTab.VAULT -> ApkVaultScreen(
                    viewModel = viewModel,
                    vaultApks = vaultApks
                )
                AppTab.BEAM_TV -> BeamToTvScreen(
                    viewModel = viewModel
                )
                AppTab.RECEIVE -> ReceiveScreen(
                    viewModel = viewModel
                )
                AppTab.TV_GUIDE -> TvGuideScreen()
            }
        }
    }

    // Dialogs
    updateInfo?.let { info ->
        AppUpdateDialog(
            updateInfo = info,
            downloadProgress = downloadProgress,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onStartUpdate = { viewModel.performAppUpdate(info) }
        )
    }

    if (showExtractDialog) {
        ExtractAppsDialog(
            installedApps = installedApps,
            isExtracting = isExtracting,
            onDismiss = { viewModel.setShowExtractDialog(false) },
            onExtractApp = { app -> viewModel.extractApp(app) }
        )
    }

    if (showDirectUrlDialog) {
        DirectUrlDialog(
            onDismiss = { viewModel.setShowDirectUrlDialog(false) },
            onDownload = { url, name -> viewModel.downloadByCustomUrl(url, name) }
        )
    }

    if (showParseErrorDialog) {
        ParseErrorGuideDialog(
            onDismiss = { viewModel.setShowParseErrorDialog(false) }
        )
    }

    selectedApkDetail?.let { apk ->
        ApkDetailDialog(
            apk = apk,
            onDismiss = { viewModel.openApkDetail(null) },
            onDownload = { viewModel.downloadApkItem(apk) },
            onBeam = {
                viewModel.setTab(AppTab.BEAM_TV)
                viewModel.openApkDetail(null)
            }
        )
    }
}

@Composable
fun TopAppBarHeader(
    serverRunning: Boolean,
    serverIp: String,
    serverPort: Int,
    isCheckingUpdate: Boolean,
    onAddUrlClick: () -> Unit,
    onHelpClick: () -> Unit,
    onCheckUpdateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FireOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "MazZe Tools",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MAZZE",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MazzePurple
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TOOLS",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MazzeCyan
                        )
                    }
                    Text(
                        text = if (serverRunning && serverIp.isNotEmpty()) "http://$serverIp:$serverPort" else "Firestick Transfer Ready",
                        fontSize = 10.sp,
                        fontFamily = if (serverRunning) FontFamily.Monospace else FontFamily.Default,
                        color = if (serverRunning) CyanAccent else TextTertiary
                    )
                }
            }

            // Quick actions with D-pad remote focus highlights
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Check for updates button
                IconButton(
                    onClick = onCheckUpdateClick,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .tvFocusHighlight(shape = CircleShape)
                        .testTag("btn_check_updates")
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MazzeCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check for Updates",
                            tint = MazzeCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onAddUrlClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .tvFocusHighlight(shape = CircleShape)
                        .testTag("btn_add_url")
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Add by URL",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .tvFocusHighlight(shape = CircleShape)
                        .testTag("btn_help_guide")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help Guide",
                        tint = FireOrangeLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.DOWNLOADS,
            onClick = { onTabSelected(AppTab.DOWNLOADS) },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Downloads") },
            label = { Text("Downloads", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = FireOrange,
                indicatorColor = FireOrange,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier
                .tvFocusHighlight(shape = RoundedCornerShape(12.dp))
                .testTag("nav_downloads")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.VAULT,
            onClick = { onTabSelected(AppTab.VAULT) },
            icon = { Icon(Icons.Default.Folder, contentDescription = "Vault") },
            label = { Text("Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = FireOrange,
                indicatorColor = FireOrange,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier
                .tvFocusHighlight(shape = RoundedCornerShape(12.dp))
                .testTag("nav_vault")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.BEAM_TV,
            onClick = { onTabSelected(AppTab.BEAM_TV) },
            icon = { Icon(Icons.Default.Send, contentDescription = "Beam TV") },
            label = { Text("Beam to TV", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = FireOrange,
                indicatorColor = FireOrange,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier
                .tvFocusHighlight(shape = RoundedCornerShape(12.dp))
                .testTag("nav_beam_tv")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.RECEIVE,
            onClick = { onTabSelected(AppTab.RECEIVE) },
            icon = { Icon(Icons.Default.Tv, contentDescription = "Receive") },
            label = { Text("Receive", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = FireOrange,
                indicatorColor = FireOrange,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier
                .tvFocusHighlight(shape = RoundedCornerShape(12.dp))
                .testTag("nav_receive")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.TV_GUIDE,
            onClick = { onTabSelected(AppTab.TV_GUIDE) },
            icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Guide") },
            label = { Text("TV Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = FireOrange,
                indicatorColor = FireOrange,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier
                .tvFocusHighlight(shape = RoundedCornerShape(12.dp))
                .testTag("nav_guide")
        )
    }
}
