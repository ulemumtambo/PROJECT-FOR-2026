package com.reportit.malawi.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * SharedComponents: I created these reusable Jetpack Compose UI components across ReportIt Malawi,
 * including navigation bars, stat cards, report item cards, dialogs, form scaffolds, and input fields.
 */
import com.reportit.malawi.R
import com.reportit.malawi.data.ReportStore
import com.reportit.malawi.model.Priority
import com.reportit.malawi.model.Report
import com.reportit.malawi.model.ReportStatus
import com.reportit.malawi.model.Role
import com.reportit.malawi.ui.theme.*
import com.reportit.malawi.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun LogoMark(size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.reportit_malawi_logo),
            contentDescription = "ReportIt Malawi logo",
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun FormScaffold(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(BackgroundLight).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Header(title, subtitle, onBack)
        Spacer(Modifier.height(22.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(20.dp), content = content)
        }
    }
}

@Composable
fun AppScaffold(nav: NavHostController, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(containerColor = BackgroundLight, bottomBar = { BottomNav(nav) }, content = content)
}

@Composable
fun Header(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        } else Spacer(Modifier.size(48.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Spacer(Modifier.size(48.dp))
    }
}

data class Tab(val label: String, val icon: ImageVector, val route: String)

@Composable
fun BottomNav(nav: NavHostController) {
    val manager = AdminAccountPolicy.isAdministratorManager(ReportStore.user.role, ReportStore.user.email)
    val officer = ReportStore.user.role == Role.OFFICER
    val home = homeRoute()
    val tabs = when {
        manager -> listOf(
            Tab("Staff", Icons.Default.People, "admin"),
            Tab("Settings", Icons.Default.Settings, "settings")
        )
        officer -> listOf(
        Tab("Dashboard", Icons.Default.Dashboard, "admin"),
        Tab("Reports", Icons.AutoMirrored.Filled.List, "adminReports"),
        Tab("Alerts", Icons.Default.Notifications, "notifications"),
        Tab("Settings", Icons.Default.Settings, "settings")
        )
        else -> listOf(
        Tab("Home", Icons.Default.Home, "citizenDashboard"),
        Tab("Reports", Icons.AutoMirrored.Filled.List, "track"),
        Tab("Alerts", Icons.Default.Notifications, "notifications"),
        Tab("Settings", Icons.Default.Settings, "settings")
        )
    }
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val unreadCount = ReportStore.myNotifications().count { !it.read }

    Surface(color = Surface, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { t ->
                val hasBadge = t.route == "notifications" && unreadCount > 0
                NavItem(t.label, t.icon, current == t.route, hasBadge = hasBadge) {
                    if (current != t.route) {
                        nav.navigate(t.route) {
                            popUpTo(home) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavItem(label: String, icon: ImageVector, selected: Boolean, hasBadge: Boolean = false, onClick: () -> Unit) {
    val tint = if (selected) GreenPrimary else TextSecondary
    Box(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Icon(icon, label, tint = tint, modifier = Modifier.size(25.dp))
                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(label, color = tint, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun StatCard(value: String, label: String, accent: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(label, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = GreenPrimary)
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
fun ReportCard(report: Report, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ReportImage(report, Modifier.size(72.dp).clip(RoundedCornerShape(11.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(report.title, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp)
                Text(report.location, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(report.status)
                }
                if (report.comments.isNotBlank() && report.comments != "No comment yet" && report.comments != "No comments yet") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Admin: ${report.comments}",
                        color = Blue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (trailing != null) trailing()
            else Text(timeAgo(report.createdAt), color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun ReportImage(report: Report, modifier: Modifier = Modifier) {
    when {
        report.photoPath != null -> PathImage(report.photoPath, modifier)
        report.imageRes != null -> Image(painterResource(report.imageRes), report.title, modifier, contentScale = ContentScale.Crop)
        else -> Box(modifier.background(GreenSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, null, tint = GreenPrimary)
        }
    }
}

@Composable
fun ReportImage(photoPath: String?, modifier: Modifier = Modifier) {
    when {
        photoPath != null -> PathImage(photoPath, modifier)
        else -> Box(modifier.background(GreenSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, null, tint = GreenPrimary)
        }
    }
}

// Loads either a local file path or a Firebase Storage URL (http/https)
@Composable
fun PathImage(path: String, modifier: Modifier = Modifier) {
    val bmp by produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                if (path.startsWith("http")) {
                    URL(path).openStream().use { BitmapFactory.decodeStream(it) }
                } else {
                    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 2 })
                }
            } catch (e: Exception) { null }
        }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), "Report photo", modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(GreenSoft), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.BrokenImage, null, tint = GreenPrimary)
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) GreenPrimary else Color.White)
            .border(1.dp, if (selected) GreenPrimary else Color(0xFFE2E7E4), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (selected) Color.White else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PhotoSlot(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.size(86.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(1.dp, Color(0xFFE1E7E3), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = GreenPrimary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
fun StatusPill(status: ReportStatus) {
    val (bg, fg) = when (status) {
        ReportStatus.PENDING -> Color(0xFFEDEDED) to Color(0xFF555555)
        ReportStatus.SUBMITTED -> Color(0xFFE1F5EA) to GreenDark
        ReportStatus.RECEIVED -> Color(0xFFDCEEFF) to Blue
        ReportStatus.IN_PROGRESS -> Color(0xFFFFE7A3) to Color(0xFF8A5A00)
        ReportStatus.RESOLVED -> Color(0xFFDDF5E7) to Color(0xFF087443)
    }
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(statusLabel(status), color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PriorityTag(priority: Priority) {
    val c = priorityColor(priority)
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(c.copy(alpha = .15f)).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }, color = c, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusDot(online: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(if (online) Color(0xFF4CAF50) else Amber))
        Spacer(Modifier.width(6.dp))
        Text(if (online) "Online" else "Offline mode", color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun StatusTracker(status: ReportStatus) {
    val steps = listOf("Submitted", "Received", "In Progress", "Resolved")
    val reached = when (status) {
        ReportStatus.PENDING -> -1
        ReportStatus.SUBMITTED -> 0
        ReportStatus.RECEIVED -> 1
        ReportStatus.IN_PROGRESS -> 2
        ReportStatus.RESOLVED -> 3
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        steps.forEachIndexed { i, s ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (i <= reached) GreenPrimary else Color(0xFFD9DEDB)),
                    contentAlignment = Alignment.Center) {
                    if (i <= reached) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(s, fontSize = 10.sp, color = if (i <= reached) GreenDark else TextSecondary)
            }
        }
    }
}

@Composable
fun CategoryBar(name: String, count: Int, total: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(name, Modifier.weight(1f), fontSize = 13.sp)
            Text("$count", fontWeight = FontWeight.Bold, color = TextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(GreenSoft)) {
            Box(Modifier.fillMaxWidth(if (total == 0) 0f else count.toFloat() / total).fillMaxHeight()
                .clip(RoundedCornerShape(99.dp)).background(GreenPrimary))
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    text: String = "",
    subtitle: String = text,
    icon: ImageVector = Icons.Default.Inbox,
    onClick: () -> Unit = {}
) {
    val message = text.ifBlank { subtitle }
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (message.isNotBlank()) {
            Text(message, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun SettingRow(
    title: String = "",
    label: String = title,
    icon: ImageVector,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val displayTitle = title.ifBlank { label }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = GreenPrimary)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(displayTitle, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
    }
}

@Composable
fun SettingSwitch(
    title: String = "",
    label: String = title,
    icon: ImageVector,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onChange: ((Boolean) -> Unit)? = null
) {
    val displayTitle = title.ifBlank { label }
    val callback = onCheckedChange ?: onChange ?: {}
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = GreenPrimary)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(displayTitle, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = callback,
            colors = SwitchDefaults.colors(checkedTrackColor = GreenPrimary)
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        if (icon != null) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontSize = 14.sp)
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    DetailRow(label = label, value = value, icon = icon)
}

@Composable
fun RequiredLabel(text: String, defaultColor: Color = Color.Black) {
    if (text.contains("*")) {
        val parts = text.split("*")
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = defaultColor)) {
                    append(parts[0])
                }
                withStyle(SpanStyle(color = Red, fontWeight = FontWeight.ExtraBold)) {
                    append("*")
                }
                if (parts.size > 1) {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(parts[1])
                    }
                }
            }
        )
    } else {
        Text(text, color = defaultColor)
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector? = null,
    placeholder: String? = null,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { RequiredLabel(label, Color.Black) },
        placeholder = placeholder?.let { { Text(it, color = Color.Black.copy(alpha = 0.7f)) } },
        leadingIcon = icon?.let { { Icon(it, null, tint = Color.Black) } },
        trailingIcon = if (password) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Show password",
                        tint = Color.Black
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors()
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    disabledTextColor = Color.Black.copy(alpha = 0.6f),
    focusedLabelColor = Color.Black,
    unfocusedLabelColor = Color.Black,
    disabledLabelColor = Color.Black.copy(alpha = 0.6f),
    focusedPlaceholderColor = Color.Black.copy(alpha = 0.7f),
    unfocusedPlaceholderColor = Color.Black.copy(alpha = 0.7f),
    focusedLeadingIconColor = Color.Black,
    unfocusedLeadingIconColor = Color.Black,
    focusedTrailingIconColor = Color.Black,
    unfocusedTrailingIconColor = Color.Black,
    focusedBorderColor = Color.Black,
    unfocusedBorderColor = Color(0xFF666666),
    cursorColor = Color.Black,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
fun InfoDialog(
    title: String,
    text: String = "",
    message: String = text,
    onClose: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val body = text.ifBlank { message }
    val callback = onDismiss ?: onClose ?: {}
    AlertDialog(
        onDismissRequest = callback,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = callback) { Text("OK", color = GreenDark) } }
    )
}
