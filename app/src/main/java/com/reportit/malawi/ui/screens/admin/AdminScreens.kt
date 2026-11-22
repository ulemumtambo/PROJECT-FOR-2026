
package com.reportit.malawi.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.reportit.malawi.model.Priority
import com.reportit.malawi.model.Report
import com.reportit.malawi.model.ReportCategory
import com.reportit.malawi.model.ReportStatus
import com.reportit.malawi.ui.components.*
import com.reportit.malawi.ui.theme.*
import com.reportit.malawi.util.*

/** Officer dashboard, report management, and status update screens. */

// Next step helper function in the report lifecycle workflow
private fun nextStep(s: ReportStatus): Pair<String, ReportStatus>? =
    when (s) {
        ReportStatus.SUBMITTED -> "Receive" to ReportStatus.RECEIVED
        ReportStatus.RECEIVED -> "Start" to ReportStatus.IN_PROGRESS
        ReportStatus.IN_PROGRESS -> "Resolve" to ReportStatus.RESOLVED
        else -> null
    }

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = Red,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddAdministratorDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    val cleanEmail = email.trim()
    val domain = cleanEmail.substringAfterLast('@', "")
    val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() &&
            domain.endsWith(".mw", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
                Text("Add officer", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Create an officer account for a trusted staff member. They can manage reports but cannot manage staff accounts. A password setup email will be sent."
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Work email ending in .mw") },
                    placeholder = { Text("name@agency.mw") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        error,
                        color = Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), cleanEmail) },
                enabled = !isLoading && name.trim().length >= 2 && validEmail
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create account")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ManageAdministratorsDialog(
    onDismiss: () -> Unit,
    onAddClick: () -> Unit
) {
    val ctx = LocalContext.current
    if (!AdminAccountPolicy.isAdministratorManager(ReportStore.user.role, ReportStore.user.email)) return
    val admins = ReportStore.adminAccounts

    LaunchedEffect(Unit) {
        ReportStore.loadAdminAccounts()
    }

    var adminToRemove by remember { mutableStateOf<String?>(null) }

    adminToRemove?.let { email ->
        ConfirmDialog(
            title = "Remove staff account?",
            message = "Remove $email from the officer portal? This permanently deletes their sign-in account.",
            confirmText = "Remove",
            onConfirm = {
                adminToRemove = null
                ReportStore.removeAdminAccount(
                    email,
                    onSuccess = {
                        Toast.makeText(ctx, "Staff account removed.", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = { adminToRemove = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Staff", fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff", tint = GreenDark)
                }
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Manage staff access to reports and the officer portal.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                if (admins.isEmpty()) {
                    Text("No staff accounts yet.", color = TextSecondary)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(admins, key = { it.email }) { admin ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(admin.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(admin.email, fontSize = 12.sp, color = TextSecondary)
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (admin.isActive) Color(0xFF18A66B) else Red)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (admin.isActive) "Active" else "Disabled",
                                                fontSize = 11.sp,
                                                color = if (admin.isActive) Color(0xFF18A66B) else Red,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = admin.isActive,
                                            onCheckedChange = { active ->
                                                ReportStore.setAdminActive(
                                                    admin.email,
                                                    active,
                                                    onSuccess = {
                                                        Toast.makeText(
                                                            ctx,
                                            if (active) "Staff access enabled." else "Staff access disabled.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    onFailure = { err ->
                                                        Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            }
                                        )
                                        IconButton(
                                            onClick = { adminToRemove = admin.email }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove staff account",
                                                tint = Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AdministratorManagementScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var showManageOfficers by rememberSaveable { mutableStateOf(false) }
    var showAddOfficer by rememberSaveable { mutableStateOf(false) }
    var addingOfficer by remember { mutableStateOf(false) }
    var addOfficerError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { ReportStore.loadAdminAccounts() }

    if (showManageOfficers) {
        ManageAdministratorsDialog(
            onDismiss = { showManageOfficers = false },
            onAddClick = {
                showManageOfficers = false
                showAddOfficer = true
            }
        )
    }

    if (showAddOfficer) {
        AddAdministratorDialog(
            isLoading = addingOfficer,
            error = addOfficerError,
            onDismiss = {
                if (!addingOfficer) {
                    showAddOfficer = false
                    addOfficerError = null
                }
            },
            onCreate = { name, email ->
                addingOfficer = true
                addOfficerError = null
                ReportStore.createAdminAccount(
                    name = name,
                    email = email,
                    onSuccess = { message ->
                        addingOfficer = false
                        showAddOfficer = false
                        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                    },
                    onFailure = { message ->
                        addingOfficer = false
                        addOfficerError = message
                    }
                )
            }
        )
    }

    AppScaffold(nav) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header("Staff Administration", "Administrator manager", null)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, tint = GreenDark)
                    Text("Officer accounts", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "Add officers, enable or disable their access, and remove staff accounts. Officers handle citizen reports; this account manages staff access.",
                        color = TextSecondary
                    )
                    Button(
                        onClick = { showManageOfficers = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage officers (${ReportStore.adminAccounts.size})")
                    }
                    OutlinedButton(
                        onClick = { showAddOfficer = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add an officer")
                    }
                }
            }
        }
    }
}

/** Officer dashboard and report handling. */
@Composable
fun AdminDashboardScreen(
    nav: NavHostController,
    onManage: () -> Unit,
    onDetails: (String) -> Unit
) {
    if (AdminAccountPolicy.isAdministratorManager(ReportStore.user.role, ReportStore.user.email)) {
        AdministratorManagementScreen(nav)
        return
    }

    val ctx = LocalContext.current

    val all = ReportStore.reports
    val total = all.size

    val newCount = all.count {
        it.status == ReportStatus.SUBMITTED ||
                it.status == ReportStatus.RECEIVED
    }

    val inProgress = all.count {
        it.status == ReportStatus.IN_PROGRESS
    }

    val resolved = all.count {
        it.status == ReportStatus.RESOLVED
    }

    val high = all.count {
        it.priority == Priority.HIGH &&
                it.status != ReportStatus.RESOLVED
    }

    val byCategory = all
        .groupingBy { it.category }
        .eachCount()
        .entries
        .sortedByDescending { it.value }

    var confirmClear by remember {
        mutableStateOf(false)
    }

    var toDelete by remember {
        mutableStateOf<Report?>(null)
    }
    // Confirm deleting an individual report.
    toDelete?.let { report ->
        ConfirmDialog(
            title = "Remove this report?",
            message = "\"${report.title}\" will be permanently deleted.",
            confirmText = "Delete",
            onConfirm = {
                toDelete = null

                ReportStore.deleteReport(report.id) {
                    Toast.makeText(
                        ctx,
                        "Report deletion failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                Toast.makeText(
                    ctx,
                    "Report removal requested.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = {
                toDelete = null
            }
        )
    }

    // Confirm clearing resolved reports.
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear resolved reports?",
            message = "This permanently deletes $resolved resolved report(s).",
            confirmText = "Delete",
            onConfirm = {
                confirmClear = false

                ReportStore.removeResolvedReports {
                    Toast.makeText(
                        ctx,
                        "Some resolved reports could not be deleted.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                Toast.makeText(
                    ctx,
                    "Resolved report deletion requested.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = {
                confirmClear = false
            }
        )
    }

    AppScaffold(nav) { padding ->

        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
            Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            item {
                Header(
                    "Roads Authority Portal",
                    "Officer Dashboard",
                    null
                )
            }

            item {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 440.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("$total", "Total", Blue, Modifier.weight(1f))
                                StatCard("$newCount", "New", Red, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard("$inProgress", "In Progress", Amber, Modifier.weight(1f))
                                StatCard("$resolved", "Resolved", Color(0xFF18A66B), Modifier.weight(1f))
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("$total", "Total", Blue, Modifier.weight(1f))
                            StatCard("$newCount", "New", Red, Modifier.weight(1f))
                            StatCard("$inProgress", "In Progress", Amber, Modifier.weight(1f))
                            StatCard("$resolved", "Resolved", Color(0xFF18A66B), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onManage,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        Text(
                            "Manage All Reports",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (resolved > 0) {
                        OutlinedButton(
                            onClick = {
                                confirmClear = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Red
                            )
                        ) {
                            Text(
                                "Clear Resolved",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (high > 0) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFDECEA)
                        )
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PriorityHigh,
                                null,
                                tint = Red
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                "$high high-priority report(s) need attention",
                                color = Red,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Reports by Category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(10.dp))

                        if (byCategory.isEmpty()) {
                            Text(
                                "No data yet",
                                color = TextSecondary
                            )
                        }

                        byCategory.forEach { (cat, count) ->
                            CategoryBar(
                                categoryLabel(cat),
                                count,
                                total
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Reports",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = onManage) {
                        Text(
                            "View All",
                            color = GreenDark
                        )
                    }
                }
            }

            items(
                all.sortedByDescending { it.createdAt }.take(5),
                key = { it.id }
            ) { report ->

                ReportCard(
                    report,
                    onClick = {
                        onDetails(report.id)
                    },
                    trailing = {
                        IconButton(
                            onClick = {
                                toDelete = report
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove report",
                                tint = Red
                            )
                        }
                    }
                )
            }
        }
        }
    }
}

/**
 * MANAGE ALL REPORTS
 */
@Composable
fun AdminReportsScreen(
    nav: NavHostController,
    onDetails: (String) -> Unit,
    onUpdate: (String) -> Unit
) {
    val ctx = LocalContext.current

    var statusFilter by rememberSaveable {
        mutableStateOf("All")
    }

    var catFilter by rememberSaveable {
        mutableStateOf<ReportCategory?>(null)
    }

    var query by rememberSaveable {
        mutableStateOf("")
    }

    var toDelete by remember {
        mutableStateOf<Report?>(null)
    }

    // Delete confirmation dialog.
    toDelete?.let { report ->

        ConfirmDialog(
            title = "Remove this report?",
            message = "\"${report.title}\" will be permanently deleted from the database. Citizens will no longer see it.",
            confirmText = "Delete",
            onConfirm = {
                toDelete = null

                ReportStore.deleteReport(report.id) {
                    Toast.makeText(
                        ctx,
                        "Could not delete report. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                Toast.makeText(
                    ctx,
                    "Report removal requested.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = {
                toDelete = null
            }
        )
    }

    val list = ReportStore.reports
        .filter { report ->

            val statusMatches = when (statusFilter) {
                "New" ->
                    report.status == ReportStatus.SUBMITTED

                "Received" ->
                    report.status == ReportStatus.RECEIVED

                "In Progress" ->
                    report.status == ReportStatus.IN_PROGRESS

                "Resolved" ->
                    report.status == ReportStatus.RESOLVED

                else -> true
            }

            val categoryMatches =
                catFilter == null || report.category == catFilter

            val searchMatches =
                query.isBlank() ||
                        report.title.contains(query, true) ||
                        report.location.contains(query, true) ||
                        report.id.contains(query, true)

            statusMatches && categoryMatches && searchMatches
        }
        .sortedWith(
            compareByDescending<Report> {
                it.priority.ordinal
            }.thenByDescending {
                it.createdAt
            }
        )

    AppScaffold(nav) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
        ) {

            Header(
                "Manage Reports",
                "${list.size} shown",
                null
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Search by title, location or ID", color = Color.Black.copy(alpha = 0.7f))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Color.Black)
                },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        "All",
                        "New",
                        "Received",
                        "In Progress",
                        "Resolved"
                    )
                ) { filter ->
                    CategoryChip(
                        filter,
                        statusFilter == filter
                    ) {
                        statusFilter = filter
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip(
                        "All categories",
                        catFilter == null
                    ) {
                        catFilter = null
                    }
                }

                items(ReportCategory.entries) { category ->
                    CategoryChip(
                        categoryLabel(category),
                        catFilter == category
                    ) {
                        catFilter = category
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (list.isEmpty()) {

                EmptyState(
                    "No reports found",
                    "Try a different filter or search.",
                    onClick = {}
                )

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {

                    items(
                        list,
                        key = { it.id }
                    ) { report ->

                        ReportCard(
                            report,
                            onClick = {
                                onDetails(report.id)
                            },
                            trailing = {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    // Receive -> Start -> Resolve.
                                    nextStep(report.status)?.let { (label, next) ->

                                        TextButton(
                                            onClick = {
                                                ReportStore.updateReport(
                                                    report.id,
                                                    next,
                                                    report.priority,
                                                    ""
                                                ) { msg ->
                                                    Toast.makeText(
                                                        ctx,
                                                        "Failed: $msg",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                                Toast.makeText(
                                                    ctx,
                                                    "Marked as ${statusLabel(next)}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ) {
                                            Text(
                                                label,
                                                color = Blue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            onUpdate(report.id)
                                        }
                                    ) {
                                        Text(
                                            "Update",
                                            color = GreenDark
                                        )
                                    }

                                    // REMOVE BUTTON FOR EVERY REPORT STATUS.
                                    IconButton(
                                        onClick = {
                                            toDelete = report
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove report",
                                            tint = Red
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * UPDATE REPORT STATUS AND PRIORITY
 */
@Composable
fun UpdateStatusScreen(
    id: String,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val report = ReportStore.find(id)

    var status by rememberSaveable(id) {
        mutableStateOf(
            report?.status ?: ReportStatus.RECEIVED
        )
    }

    var priority by rememberSaveable(id) {
        mutableStateOf(
            report?.priority ?: Priority.MEDIUM
        )
    }

    var comment by rememberSaveable(id) {
        mutableStateOf("")
    }

    var loaded by rememberSaveable(id) {
        mutableStateOf(report != null)
    }

    LaunchedEffect(report) {
        if (report != null && !loaded) {
            status = report.status
            priority = report.priority
            loaded = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Header(
            "Update Status",
            "#$id",
            onBack
        )

        Spacer(Modifier.height(16.dp))

        if (report == null) {
            Text(
                "Loading report...",
                color = TextSecondary
            )
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                Spacer(Modifier.height(5.dp))
                Text(report.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Choose a clear status and leave a short update for the citizen.", fontSize = 12.sp, color = TextSecondary)
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Status",
            fontWeight = FontWeight.Bold
        )

        Text("This update will appear in the citizen’s report tracking.", fontSize = 12.sp, color = TextSecondary)

        ReportStatus.entries
            .filter { it != ReportStatus.PENDING }
            .forEach { s ->

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (status == s) GreenSoft
                            else Color.White
                        )
                        .clickable {
                            status = s
                        }
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(s)

                    Spacer(Modifier.width(12.dp))

                    Text(
                        statusLabel(s),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

        Spacer(Modifier.height(14.dp))

        Text(
            "Priority",
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Priority.entries.forEach { p ->

                CategoryChip(
                    p.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    priority == p
                ) {
                    priority = p
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = {
                comment = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            label = {
                Text("Officer comment", color = Color.Black)
            },
            placeholder = {
                Text("Add an update for the citizen...", color = Color.Black.copy(alpha = 0.7f))
            },
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                ReportStore.updateReport(
                    id,
                    status,
                    priority,
                    comment
                ) { msg ->
                    Toast.makeText(
                        ctx,
                        "Update failed: $msg",
                        Toast.LENGTH_LONG
                    ).show()
                }

                Toast.makeText(
                    ctx,
                    "Report update requested",
                    Toast.LENGTH_SHORT
                ).show()

                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary
            )
        ) {
            Text(
                "Save Update",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
