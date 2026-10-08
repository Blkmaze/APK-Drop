package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FireOrange
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.MazzeCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.tvFocusHighlight
import com.example.util.AppUpdateInfo
import com.example.util.DownloadProgress
import com.example.util.NetworkUtils

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadProgress: DownloadProgress,
    onDismiss: () -> Unit,
    onStartUpdate: () -> Unit
) {
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BackHandler {
        if (!downloadProgress.isDownloading) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!downloadProgress.isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !downloadProgress.isDownloading,
            dismissOnClickOutside = !downloadProgress.isDownloading
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, MazzeCyan, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with Update Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FireOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "New Update Available!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tag: ${updateInfo.releaseTag.ifEmpty { "latest" }}",
                            fontSize = 13.sp,
                            color = CyanAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Release info container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = if (updateInfo.releaseName.isNotBlank()) updateInfo.releaseName else "APK Drop Update",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (updateInfo.apkUpdatedAt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Build timestamp: ${updateInfo.apkUpdatedAt}",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        if (updateInfo.apkSizeBytes > 0) {
                            Text(
                                text = "Size: ${NetworkUtils.formatFileSize(updateInfo.apkSizeBytes)}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = updateInfo.releaseNotes.take(300),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Download Progress (if active)
                if (downloadProgress.isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading APK-Drop.apk...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${(downloadProgress.progressPercent * 100).toInt()}%",
                                color = FireOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = FireOrange,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons with TV remote focus highlight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!downloadProgress.isDownloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .tvFocusHighlight(shape = RoundedCornerShape(10.dp))
                                .testTag("btn_update_later"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Later", fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = onStartUpdate,
                        enabled = !downloadProgress.isDownloading,
                        modifier = Modifier
                            .focusRequester(initialFocusRequester)
                            .tvFocusHighlight(shape = RoundedCornerShape(10.dp))
                            .testTag("btn_update_now"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FireOrange,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (downloadProgress.isDownloading) "Downloading..." else "Update Now",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
