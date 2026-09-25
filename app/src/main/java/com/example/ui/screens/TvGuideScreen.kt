package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeLight
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TvGuideScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = FireOrange, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Firestick & TV Setup Guide", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Essential steps to install and beam APKs seamlessly", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        // Step 1: Enable Developer Options
        item {
            GuideStepCard(
                stepNumber = "1",
                icon = Icons.Default.DeveloperMode,
                iconColor = FireOrange,
                title = "Unlock Developer Options on Fire TV",
                description = "On newer Fire OS updates, Developer Options is hidden by default.\n\n" +
                        "1. Go to Firestick Settings (Gear icon on far right).\n" +
                        "2. Select \"My Fire TV\" (or Device & Software).\n" +
                        "3. Click on \"About\".\n" +
                        "4. Highlight the device name (e.g. Fire TV Stick 4K) and click the Select button on your remote 7 TIMES until you see \"No need, you are already a developer.\""
            )
        }

        // Step 2: Install Unknown Apps
        item {
            GuideStepCard(
                stepNumber = "2",
                icon = Icons.Default.Security,
                iconColor = CyanAccent,
                title = "Allow Install Unknown Apps",
                description = "Once Developer Options is unlocked:\n\n" +
                        "1. Press Back to return to \"My Fire TV\".\n" +
                        "2. Click \"Developer Options\".\n" +
                        "3. Select \"Install unknown apps\" (or Apps from Unknown Sources).\n" +
                        "4. Turn it ON for \"Downloader\", \"Silk Browser\", and \"APK Drop\"."
            )
        }

        // Step 3: Parse Error Solutions
        item {
            GuideStepCard(
                stepNumber = "3",
                icon = Icons.Default.Warning,
                iconColor = AmberAlert,
                title = "Fixing \"There was a problem parsing the package\"",
                description = "The dreaded Parse Error happens when:\n\n" +
                        "• Architecture Mismatch: Most standard Firestick models (Fire TV Stick Lite, Stick 4K Max 1st gen) run 32-bit (armeabi-v7a). If you try to install a 64-bit-only (arm64-v8a) APK, it will fail.\n" +
                        "• Android API Level: Fire OS 7 is based on Android 9 (Pie). If an app requires Android 10 or 11 minimum, Fire OS cannot install it.\n" +
                        "• Corrupted or Incomplete Download: Check your Wi-Fi and re-download from the APK Drop transfer URL."
            )
        }

        // Step 4: Transfer Methods
        item {
            GuideStepCard(
                stepNumber = "4",
                icon = Icons.Default.Build,
                iconColor = GreenSuccess,
                title = "How to Transfer Without Any Cables",
                description = "Two super simple methods:\n\n" +
                        "• Method A (Browser / Downloader): Open \"Beam to TV\" tab on this phone. Open Downloader on Firestick, enter the IP (e.g. http://192.168.1.XX:8888) and 1-tap download!\n\n" +
                        "• Method B (6-Digit PIN): Open APK Drop on both devices on the same Wi-Fi. Enter the 6-digit PIN on the TV Receive screen to pull the app directly."
            )
        }
    }
}

@Composable
fun GuideStepCard(
    stepNumber: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("STEP $stepNumber", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = iconColor)
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}
