package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.DiscoveredDevice
import com.example.network.ServerStatus
import com.example.ui.ApkDropViewModel
import com.example.ui.components.QrCodeView
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.FireOrangeLight
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.NetworkUtils

@Composable
fun BeamToTvScreen(
    viewModel: ApkDropViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val quickCode by viewModel.activeQuickCode.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()

    val serverUrl = if (serverStatus.ipAddress.isNotEmpty()) {
        "http://${serverStatus.ipAddress}:${serverStatus.port}"
    } else {
        "http://192.168.1.xxx:8888"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Server Status & Power Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (serverStatus.isRunning) GreenSuccess.copy(alpha = 0.5f) else BorderSubtle),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (serverStatus.isRunning) GreenSuccess.copy(alpha = 0.15f) else RedWarning.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (serverStatus.isRunning) Icons.Default.Wifi else Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = if (serverStatus.isRunning) GreenSuccess else RedWarning,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (serverStatus.isRunning) "BEAM SERVER ACTIVE" else "SERVER OFFLINE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (serverStatus.isRunning) GreenSuccess else RedWarning
                                )
                                Text(
                                    text = if (serverStatus.isRunning) "Broadcasting on Wi-Fi" else "Tap toggle to turn on",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = serverStatus.isRunning,
                            onCheckedChange = { isChecked ->
                                if (isChecked) viewModel.startTransferServer() else viewModel.stopTransferServer()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GreenSuccess,
                                checkedTrackColor = Color(0xFF003822),
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.testTag("server_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // TV Downloader App URL Box (Prominent & High Contrast)
                    Text(
                        text = "FIRESTICK DOWNLOADER & BROWSER URL:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FireOrangeLight,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A0D14))
                            .border(1.dp, FireOrangeDark, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = serverUrl,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("MazZe Tools URL", serverUrl))
                                    Toast.makeText(context, "Copied URL: $serverUrl", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = FireOrange)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "📺 Type this address directly into the Downloader app on your Fire TV or Silk Browser to download all APKs without any cables!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 6-Digit Quick Transfer Code (Squiffix / Send Files to TV style)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QUICK TRANSFER CODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = quickCode,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FireOrange,
                            letterSpacing = 6.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(onClick = { viewModel.generateNewQuickCode() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Code", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter this 6-digit code in MazZe Tools on your TV or other device's Receive tab.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // QR Code for Instant Mobile / Tablet Pairing
                    QrCodeView(
                        content = serverUrl,
                        size = 160.dp,
                        darkColor = Color.Black,
                        lightColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Scan QR with Phone or Tablet on same Wi-Fi",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Live LAN Transfer Stats & Activity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Live Transfer Activity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${serverStatus.connectedClientsCount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Text("Connected Clients", fontSize = 11.sp, color = TextTertiary)
                        }

                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderSubtle))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = NetworkUtils.formatFileSize(serverStatus.totalTransferredBytes),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess
                            )
                            Text("Data Transferred", fontSize = 11.sp, color = TextTertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Status: ${serverStatus.lastEvent}",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Discovered Devices on LAN
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = FireOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Discovered Devices on Wi-Fi (${discoveredDevices.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (discoveredDevices.isEmpty()) {
                        Text(
                            text = "Scanning local network for Firesticks, Android TVs, and other phones running MazZe Tools...",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            lineHeight = 16.sp
                        )
                    } else {
                        discoveredDevices.forEach { dev ->
                            DiscoveredDeviceRow(device = dev)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredDeviceRow(device: DiscoveredDevice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(device.deviceName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Text("IP: ${device.ipAddress} • Port ${device.port}", fontSize = 11.sp, color = TextTertiary)
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF003822))
                .border(1.dp, GreenSuccess, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("Ready", color = GreenSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
