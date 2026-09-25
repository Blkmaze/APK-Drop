package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ParseErrorGuideDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberAlert.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Parse Error Diagnostic Guide", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Why Android TV & Fire OS refuse some APKs", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        DiagnosticSection(
                            title = "1. Architecture (armeabi-v7a vs arm64-v8a)",
                            body = "Amazon Fire TV Stick Lite, Firestick 3rd Gen, and many budget Android TV boxes run 32-bit (armeabi-v7a) operating systems even with 64-bit hardware.\n\n" +
                                    "If an APK only contains 64-bit binaries (arm64), Android displays: \"There was a problem parsing the package\".\n\n" +
                                    "Fix: Always download the 32-bit (armeabi-v7a or universal) version of apps (e.g. Kodi, VLC, SmartTube) for Firestick."
                        )
                    }

                    item {
                        DiagnosticSection(
                            title = "2. Unknown Sources Not Enabled",
                            body = "Fire OS blocks APK installation if the installer application does not have permission.\n\n" +
                                    "Fix: Settings > My Fire TV > Developer Options > Install unknown apps > Toggle ON for Downloader, Silk Browser, and APK Drop."
                        )
                    }

                    item {
                        DiagnosticSection(
                            title = "3. Incompatible Android Version (minSdkVersion)",
                            body = "Fire OS 7 is based on Android 9.0 (API 28). Fire OS 8 is Android 11.\n\n" +
                                    "If a newer APK targets a minimum of Android 12 or 13, the package installer on Fire OS will reject it immediately with a parse error."
                        )
                    }

                    item {
                        DiagnosticSection(
                            title = "4. Corrupted or Partial Download",
                            body = "If Wi-Fi disconnected before 100% completion, the APK archive zip header will be invalid.\n\n" +
                                    "Fix: In APK Drop, tap Delete on the corrupted vault item and re-download."
                        )
                    }

                    item {
                        DiagnosticSection(
                            title = "5. Clearing Package Installer Cache",
                            body = "Sometimes the Fire TV package installer daemon hangs.\n\n" +
                                    "Fix: Settings > Applications > Manage Installed Applications > Show System Applications > Package Installer > Clear Cache & Force Stop, then reboot device."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got It, Close Guide", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSection(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(14.dp)
    ) {
        Column {
            Text(title, color = FireOrangeLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
