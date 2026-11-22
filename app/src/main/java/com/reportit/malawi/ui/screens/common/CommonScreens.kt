package com.reportit.malawi.ui.screens.common

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.reportit.malawi.data.ReportStore
import com.reportit.malawi.model.Role
import com.reportit.malawi.ui.components.*
import com.reportit.malawi.ui.theme.*
import com.reportit.malawi.util.*

/**
 * CommonScreens: I created this file to contain shared screens accessible to both Citizens and Officers,
 * including Report Details View, Real-Time Notifications Center, and Settings / Account Management.
 */

// ---------------------------------------------------------
// REPORT DETAILS SCREEN
// ---------------------------------------------------------

@Composable
fun ReportDetailsScreen(
    id: String,
    onBack: () -> Unit,
    onUpdate: (String) -> Unit
) {
    // Observe the reports list so the screen refreshes when a report changes.
    val report by remember(id) {
        derivedStateOf {
            ReportStore.find(id)
        }
    }

    val context = LocalContext.current

    if (report == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Header(
                title = "Report Details",
                onBack = onBack
            )

            EmptyState(
                icon = Icons.Default.Description,
                title = "Report not found",
                subtitle = "This report may have been removed or is no longer available."
            )
        }

        return
    }

    val currentReport = report!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Header(
            title = "Report Details",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReportImage(
                        photoPath = currentReport.photoPath
                    )

                    Text(
                        text = currentReport.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = currentReport.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            status = currentReport.status
                        )

                        PriorityTag(
                            priority = currentReport.priority
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Report Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    StatusTracker(
                        status = currentReport.status
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Report Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    DetailRow(
                        label = "Category",
                        value = categoryLabel(currentReport.category)
                    )

                    DetailRow(
                        label = "Location",
                        value = currentReport.location
                    )

                    DetailRow(
                        label = "Landmark",
                        value = currentReport.landmark.ifBlank {
                            "Not provided"
                        }
                    )

                    DetailRow(
                        label = "Description",
                        value = currentReport.description
                    )

                    DetailRow(
                        label = "Reported by",
                        value = currentReport.citizenName
                    )

                    DetailRow(
                        label = "Email",
                        value = currentReport.citizenEmail
                    )

                    DetailRow(
                        label = "Reported on",
                        value = timeAgo(currentReport.createdAt)
                    )

                    DetailRow(
                        label = "Comments",
                        value = currentReport.comments.ifBlank {
                            "No comments yet"
                        }
                    )
                }
            }

            // Open the report coordinates in an installed map application.
            if (
                currentReport.latitude != null &&
                currentReport.longitude != null
            ) {
                Button(
                    onClick = {
                        val latitude = currentReport.latitude
                        val longitude = currentReport.longitude

                        val mapUri = Uri.parse(
                            "geo:0,0?q=$latitude,$longitude(${Uri.encode(currentReport.title)})"
                        )

                        val mapIntent = Intent(
                            Intent.ACTION_VIEW,
                            mapUri
                        )

                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "No map application is available.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Open Location in Maps")
                }
            }

            // Only officers should see the update action.
            if (ReportStore.user.role != Role.CITIZEN) {
                Button(
                    onClick = {
                        onUpdate(currentReport.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Update Status")
                }
            }
        }
    }
}


// ---------------------------------------------------------
// NOTIFICATIONS SCREEN (LIVE ALERTS ENGINE)
// ---------------------------------------------------------

@Composable
fun NotificationsScreen(
    nav: NavHostController
) {
    LaunchedEffect(Unit) {
        ReportStore.seedDefaultNotificationsIfNeeded()
    }

    val currentUser = ReportStore.user
    var filterUnreadOnly by remember { mutableStateOf(false) }
    var notificationToDelete by remember { mutableStateOf<com.reportit.malawi.model.AppNotification?>(null) }

    val allList = ReportStore.notifications.filter { notification ->
        notification.audience == "ALL" ||
                notification.audience.equals(currentUser.email, ignoreCase = true) ||
                (currentUser.role != Role.CITIZEN && notification.audience == "OFFICERS")
    }

    val displayedList = if (filterUnreadOnly) {
        allList.filter { !it.read }
    } else {
        allList
    }

    val unreadCount = allList.count { !it.read }

    AppScaffold(nav) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
        ) {
            Header(
                title = "Alerts & Notifications",
                subtitle = if (unreadCount > 0) "$unreadCount unread updates" else "All caught up"
            )

            // ACTION BAR & FILTERS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !filterUnreadOnly,
                        onClick = { filterUnreadOnly = false },
                        label = { Text("All (${allList.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = filterUnreadOnly,
                        onClick = { filterUnreadOnly = true },
                        label = { Text("Unread ($unreadCount)", fontSize = 12.sp) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (unreadCount > 0) {
                        IconButton(onClick = { ReportStore.markAllNotificationsAsRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = GreenPrimary)
                        }
                    }
                    if (allList.isNotEmpty()) {
                        IconButton(onClick = { ReportStore.clearAllNotifications() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = TextSecondary)
                        }
                    }
                }
            }

            if (displayedList.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = if (filterUnreadOnly) "No unread alerts" else "No notifications yet",
                    subtitle = "Updates regarding reported infrastructure damages will appear here in real time."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedList, key = { it.id }) { notification ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ReportStore.markNotificationAsRead(notification.id)
                                    notification.reportId?.let { repId ->
                                        if (repId.isNotBlank()) {
                                            nav.navigate("details/$repId")
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!notification.read) Color(0xFFEBF3FF) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (!notification.read) Blue.copy(alpha = 0.15f) else Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (!notification.read) Blue else TextSecondary
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (notification.audience == "OFFICERS") "Officer Alert" else "Report Status Alert",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Blue
                                        )
                                        if (!notification.read) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Red, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "1",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = notification.message,
                                        fontSize = 13.sp,
                                        fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.Normal,
                                        color = TextPrimary
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = timeAgo(notification.time),
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )

                                        if (notification.reportId != null) {
                                            Text(
                                                text = "Tap to view report >",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Blue
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { notificationToDelete = notification },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete alert",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (notificationToDelete != null) {
        AlertDialog(
            onDismissRequest = { notificationToDelete = null },
            title = { Text("Delete Notification?") },
            text = { Text("Are you sure you want to delete this notification? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        notificationToDelete?.let { ReportStore.clearNotification(it.id) }
                        notificationToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { notificationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ---------------------------------------------------------
// SETTINGS SCREEN
// ---------------------------------------------------------

@Composable
fun SettingsScreen(
    nav: NavHostController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val user = ReportStore.user

    var dialog by remember {
        mutableStateOf<String?>(null)
    }

    var newName by remember(user.name) {
        mutableStateOf(user.name)
    }

    var savingProfile by remember {
        mutableStateOf(false)
    }

    AppScaffold(nav) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            Surface(
                color = Background,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(16.dp)) {
                    Header(title = "Settings")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PROFILE CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.name
                                    .split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .joinToString("") { it.first().uppercase() }
                                    .ifBlank { "U" },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Text(
                                text = when (user.role) {
                                    Role.ADMIN -> "Administrator"
                                    Role.OFFICER -> "Authority Officer"
                                    Role.CITIZEN -> "Citizen"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary
                            )
                        }
                    }
                }

                // ACCOUNT SETTINGS
                SettingsSection(title = "Account") {
                    SettingRow(
                        icon = Icons.Default.Person,
                        title = "My Profile",
                        subtitle = "Update your personal information",
                        onClick = {
                            newName = user.name
                            dialog = "profile"
                        }
                    )
                }

                // APP PREFERENCES
                SettingsSection(title = "Preferences") {
                    SettingSwitch(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Receive report updates",
                        checked = ReportStore.notificationsOn,
                        onCheckedChange = {
                            ReportStore.notificationsOn = it
                        }
                    )

                    HorizontalDivider(
                        color = DividerColor.copy(alpha = 0.5f)
                    )

                    SettingSwitch(
                        icon = Icons.Default.Sync,
                        title = "Auto-sync",
                        subtitle = "Automatically sync pending reports when supported",
                        checked = ReportStore.autoSync,
                        onCheckedChange = {
                            ReportStore.autoSync = it
                        }
                    )
                }

                // SUPPORT
                SettingsSection(title = "Support") {
                    SettingRow(
                        icon = Icons.Default.Security,
                        title = "Privacy & Security",
                        subtitle = "Learn how your information is handled",
                        onClick = {
                            dialog = "privacy"
                        }
                    )

                    HorizontalDivider(
                        color = DividerColor.copy(alpha = 0.5f)
                    )

                    SettingRow(
                        icon = Icons.AutoMirrored.Filled.Help,
                        title = "Help & Support",
                        subtitle = "Get help using ReportIt Malawi",
                        onClick = {
                            dialog = "help"
                        }
                    )

                    HorizontalDivider(
                        color = DividerColor.copy(alpha = 0.5f)
                    )

                    SettingRow(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "About ReportIt Malawi",
                        onClick = {
                            dialog = "about"
                        }
                    )
                }
            }

            // Fixed bottom logout action container
            Surface(
                color = Surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("Logout", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // -----------------------------------------------------
    // PROFILE DIALOG
    // -----------------------------------------------------

    if (dialog == "profile") {
        AlertDialog(
            onDismissRequest = {
                if (!savingProfile) {
                    dialog = null
                }
            },
            title = {
                Text("Edit Profile")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Update your name.",
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Full name", color = Color.Black)
                        },
                        singleLine = true,
                        enabled = !savingProfile,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !savingProfile && newName.trim().isNotBlank(),
                    onClick = {
                        savingProfile = true

                        ReportStore.updateProfileName(
                            newName = newName,
                            onSuccess = {
                                savingProfile = false
                                dialog = null

                                Toast.makeText(
                                    context,
                                    "Profile updated successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { message ->
                                savingProfile = false

                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                ) {
                    if (savingProfile) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !savingProfile,
                    onClick = {
                        dialog = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // -----------------------------------------------------
    // INFORMATION DIALOGS
    // -----------------------------------------------------

    when (dialog) {
        "privacy" -> PrivacySecurityDialog(onDismiss = { dialog = null })
        "help" -> HelpSupportDialog(onDismiss = { dialog = null })
        "about" -> AboutAppDialog(onDismiss = { dialog = null })
    }
}

// ---------------------------------------------------------
// PROFESSIONAL INFORMATION DIALOGS
// ---------------------------------------------------------

@Composable
fun PrivacySecurityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = GreenPrimary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Privacy & Security Policy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text("Republic of Malawi - Roads Authority", fontSize = 11.sp, color = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GreenDark)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Official Data Protection: Compliant with Electronic Transactions & Cybersecurity Act (2016)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenDark
                        )
                    }
                }

                Text("1. Data Encryption & Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(
                    "All transmissions between ReportIt Malawi and central government servers are encrypted using TLS 1.3 protocol. Offline local data stored on your device is protected by Android KeyStore hardware-backed cryptography.",
                    fontSize = 12.sp,
                    color = Color.Black
                )

                Text("2. Location & Camera Data Usage", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(
                    "GPS coordinates and captured photos are accessed solely when submitting an infrastructure report. This information enables regional engineering teams (Northern, Central, and Southern zones) to locate exact hazard sites.",
                    fontSize = 12.sp,
                    color = Color.Black
                )

                Text("3. Access Control & User Identity", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(
                    "Citizen profiles and report logs are private. Public users can only inspect their own submitted incidents, while authorized Roads Authority officers have restricted role-based administrative access.",
                    fontSize = 12.sp,
                    color = Color.Black
                )

                Text("4. Data Retention Policy", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(
                    "Infrastructure reports are archived for up to 5 years for road maintenance planning and public safety audits. Account data is never shared with third-party advertisers.",
                    fontSize = 12.sp,
                    color = Color.Black
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:security@roadsauthority.mw")
                                putExtra(Intent.EXTRA_SUBJECT, "Security & Privacy Concern - ReportIt Malawi")
                            }
                            context.startActivity(intent)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Contact Security Team: security@roadsauthority.mw", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GreenDark)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("I Understand", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun HelpSupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = GreenPrimary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Help & Technical Support", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text("Roads Authority Assistance Desk", fontSize = 11.sp, color = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Direct Support Channels", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:199"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenDark)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Call Toll-Free Hotline: 199", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@roadsauthority.mw")
                            putExtra(Intent.EXTRA_SUBJECT, "Support Inquiry - ReportIt Malawi")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenDark)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Email: support@roadsauthority.mw", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Text("Frequently Asked Questions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FaqItem(
                        question = "1. How does offline reporting work?",
                        answer = "A: You can submit road hazard reports even without active connection. Reports are saved locally and synced automatically when internet connection is restored."
                    )
                    FaqItem(
                        question = "2. What is the response SLA for reports?",
                        answer = "A: Emergency hazards (e.g. collapsed bridges, major flooding) are assessed within 24 hours. Standard repairs are scheduled with regional engineering offices."
                    )
                    FaqItem(
                        question = "3. What do the status tags indicate?",
                        answer = "A: Submitted (Received by server) → Received (Assigned to officer) → In Progress (Crews working) → Resolved (Work complete)."
                    )
                }

                HorizontalDivider()

                Text("Headquarters Address:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
                Text(
                    "Roads Authority Headquarters, Off Paul Kagame Road, Private Bag B346, Lilongwe, Malawi",
                    fontSize = 11.sp,
                    color = Color.Black
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6F5))
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(question, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(answer, fontSize = 11.sp, color = Color.Black)
        }
    }
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                LogoMark(64.dp)
                Spacer(Modifier.height(8.dp))
                Text("ReportIt Malawi", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = GreenDark)
                Text("Official Civic Infrastructure Portal", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F8F0),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "Version 1.0.4 (Build 1042-PROD)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF13794F),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    "ReportIt Malawi is the official national mobile infrastructure reporting system designed to empower citizens across Malawi to report road damages, washed-out bridges, blocked drainage, and streetlighting issues directly to authorities.",
                    fontSize = 12.sp,
                    color = Color.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                HorizontalDivider()

                Text("Partner Institutions & Stakeholders", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PartnerBullet("Ministry of Transport and Public Works")
                    PartnerBullet("Roads Authority Malawi (RA)")
                    PartnerBullet("Roads Fund Administration (RFA)")
                    PartnerBullet("City & District Councils (Lilongwe, Blantyre, Mzuzu, Zomba)")
                }

                HorizontalDivider()

                Text("Core Application Features", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PartnerBullet("Offline-First Caching & Automatic Sync Engine")
                    PartnerBullet("GPS Coordinate Pinpointing & Photo Evidence Upload")
                    PartnerBullet("Real-Time Incident Tracking & Notification Alerts")
                    PartnerBullet("Role-Based Dashboard for Municipal & Highways Officers")
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    "© 2026 Government of Malawi. All Rights Reserved.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PartnerBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, color = Color.Black)
    }
}


// ---------------------------------------------------------
// SETTINGS SECTION
// ---------------------------------------------------------

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp),
                content = content
            )
        }
    }
}
