package com.reportit.malawi.ui.screens.citizen

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.reportit.malawi.R
import com.reportit.malawi.data.ReportStore
import com.reportit.malawi.model.Report
import com.reportit.malawi.model.ReportCategory
import com.reportit.malawi.model.ReportStatus
import com.reportit.malawi.ui.components.*
import com.reportit.malawi.ui.theme.*
import com.reportit.malawi.util.*
import java.io.File
import java.util.Locale

/**
 * CitizenScreens:
 * Dashboard, Report Problem Form, and Track My Reports screens.
 */

@Composable
fun CitizenDashboardScreen(
    nav: NavHostController,
    onReport: () -> Unit,
    onDetails: (String) -> Unit
) {
    val ctx = LocalContext.current
    val mine = ReportStore.myReports()
    val pending = ReportStore.pendingCount()
    val online = isOnline(ctx)

    val firstName = ReportStore.user.name
        .substringBefore(" ")
        .ifBlank { "Citizen" }

    AppScaffold(nav) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .statusBarsPadding()
        ) {
            Surface(
                color = BackgroundLight,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Citizen Portal",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                "Welcome back, $firstName",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        LogoMark(36.dp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.malawi_road_splash),
                            contentDescription = "Malawi road",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0x88000000))
                        )

                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Hello, $firstName",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    "Report road problems and follow progress in your area.",
                                    color = Color.White.copy(alpha = .9f),
                                    fontSize = 13.sp
                                )

                                Spacer(Modifier.height(10.dp))
                                StatusDot(online)
                            }

                            LogoMark(56.dp)
                        }
                    }
                }

                if (pending > 0) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3CD)
                            )
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFF8A5A00)
                                )

                                Spacer(Modifier.width(10.dp))

                                Text(
                                    "$pending report(s) waiting to sync",
                                    Modifier.weight(1f),
                                    color = Color(0xFF8A5A00),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                TextButton(
                                    onClick = {
                                        if (isOnline(ctx)) {
                                            val n = ReportStore.syncPending()

                                            Toast.makeText(
                                                ctx,
                                                "Syncing $n report(s)...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                ctx,
                                                "No internet connection yet",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ) {
                                    Text(
                                        "Sync now",
                                        color = GreenDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.padding(top = 2.dp)) {
                        Text(
                            "Your reports",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Track the progress of issues you have reported.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            "${mine.size}",
                            "My Reports",
                            Blue,
                            Modifier.weight(1f)
                        )

                        StatCard(
                            "${mine.count { it.status == ReportStatus.IN_PROGRESS }}",
                            "In Progress",
                            Amber,
                            Modifier.weight(1f)
                        )

                        StatCard(
                            "${mine.count { it.status == ReportStatus.RESOLVED }}",
                            "Resolved",
                            Color(0xFF18A66B),
                            Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Button(
                        onClick = onReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Report a Road Problem",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEAF5EE)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GreenDark,
                                    modifier = Modifier
                                        .padding(11.dp)
                                        .size(22.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    "Help the road team locate the issue",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "When safe, include a nearby landmark and a clear photo.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
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
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        TextButton(
                            onClick = { nav.navigate("track") }
                        ) {
                            Text("View all", color = GreenDark)
                        }
                    }
                }

                if (mine.isEmpty()) {
                    item {
                        EmptyState(
                            "No reports yet",
                            "Tap \"Report a Road Problem\" to send your first report.",
                            onClick = onReport
                        )
                    }
                } else {
                    items(mine.take(3)) { report ->
                        ReportCard(
                            report,
                            onClick = { onDetails(report.id) }
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------
   REPORT FORM SCREEN
   --------------------------------------------------------- */

@Composable
fun ReportFormScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val ctx = LocalContext.current

    var selected by rememberSaveable {
        mutableStateOf(ReportCategory.DETERIORATING_ROADS)
    }

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }

    var titleError by rememberSaveable { mutableStateOf("") }
    var descError by rememberSaveable { mutableStateOf("") }
    var locationError by rememberSaveable { mutableStateOf("") }

    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lng by rememberSaveable { mutableStateOf<Double?>(null) }

    var locating by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }

    // CAMERA
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPath

        if (success && path != null && File(path).length() > 0) {
            photoPath = path
            error = ""
        } else if (path != null) {
            File(path).delete()
        }

        pendingPath = null
    }

    // GALLERY
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val path = copyUriToPhoto(ctx, uri)

            if (path != null) {
                photoPath = path
                error = ""
            } else {
                Toast.makeText(
                    ctx,
                    "Could not load image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // GPS LOCATION
    val doLocate: () -> Unit = {
        if (!locating) {
            locating = true

            fetchLocation(ctx) { loc ->
                locating = false

                if (loc != null) {
                    lat = loc.latitude
                    lng = loc.longitude

                    error = ""
                } else {
                    Toast.makeText(
                        ctx,
                        "Could not get location. Turn on GPS or enter it manually.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (
                results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                doLocate()
            } else {
                Toast.makeText(
                    ctx,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .statusBarsPadding()
    ) {
        Surface(
            color = BackgroundLight,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.padding(16.dp)) {
                Header(
                    "Report a Road Problem",
                    "Citizen",
                    onBack
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Text(
                "Help improve road safety in your community.",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F3EC))
            ) {
                Text(
                    "No internet? Your report will be saved on this phone and sent when you reconnect.",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    fontSize = 12.sp,
                    color = GreenDark
                )
            }

            Spacer(Modifier.height(20.dp))

            // CATEGORY
            Text(
                "1. Problem Category",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Select the type of problem you want to report.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            var categoryExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { categoryExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(categoryLabel(selected), fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    ReportCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    categoryLabel(category),
                                    fontWeight = if (selected == category) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected == category) GreenDark else TextPrimary
                                )
                            },
                            onClick = {
                                selected = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // TITLE
            Text(
                "2. Report Details",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(12.dp))

            Field(
                "Title *",
                title,
                {
                    title = it
                    titleError = ""
                    error = ""
                },
                null,
                placeholder = "e.g. Damaged road near the market"
            )
            if (titleError.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(titleError, color = Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    descError = ""
                    error = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                label = { RequiredLabel("Description *", Color.Black) },
                placeholder = {
                    Text(
                        "Describe the problem, its condition and how it affects road users...",
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                minLines = 4,
                maxLines = 5
            )
            if (descError.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(descError, color = Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Provide enough information to help the responsible authority understand the problem.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(22.dp))

            // PHOTO
            Text(
                "3. Attach a Photo",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "A photo helps the authority understand the reported problem.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhotoSlot(
                    Icons.Default.PhotoCamera,
                    "Camera"
                ) {
                    try {
                        val file = File(
                            ctx.filesDir,
                            "report_${System.currentTimeMillis()}.jpg"
                        )

                        pendingPath = file.absolutePath

                        val uri = FileProvider.getUriForFile(
                            ctx,
                            "${ctx.packageName}.fileprovider",
                            file
                        )

                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        pendingPath = null

                        Toast.makeText(
                            ctx,
                            "Camera not available: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                PhotoSlot(
                    Icons.Default.Image,
                    "Gallery"
                ) {
                    galleryLauncher.launch("image/*")
                }
            }

            if (photoPath != null) {
                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(100.dp)) {
                        PathImage(
                            photoPath!!,
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )

                        IconButton(
                            onClick = {
                                photoPath?.let { File(it).delete() }
                                photoPath = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Remove photo",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            "Photo attached",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Text(
                            "You can remove it before submitting.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))

                Text(
                    "Photo attachment is optional.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(22.dp))

            // LOCATION
            Text(
                "4. Problem Location",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Enter the location place or district, press GPS if you are on site, then submit.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            Field(
                "Location / Area / District *",
                location,
                {
                    location = it
                    locationError = ""
                    error = ""
                },
                Icons.Default.LocationOn,
                placeholder = "e.g. Blantyre Market or Lilongwe Area 3"
            )
            if (locationError.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(locationError, color = Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    if (locating) return@OutlinedButton

                    if (hasLocationPermission(ctx)) {
                        doLocate()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !locating && !submitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GreenDark
                )
            ) {
                if (locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    if (locating) "Getting location..." else "Capture current GPS location (If on site)"
                )
            }

            if (lat != null && lng != null) {
                Spacer(Modifier.height(6.dp))

                Text(
                    "GPS coordinates captured: %.5f, %.5f".format(
                        Locale.US,
                        lat,
                        lng
                    ),
                    color = GreenDark,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Field(
                "Nearby landmark (optional)",
                landmark,
                { landmark = it },
                Icons.Default.Place,
                placeholder = "e.g. Near the main market or school"
            )

            Spacer(Modifier.height(22.dp))

            // ERROR MESSAGE
            if (error.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Red
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            error,
                            color = Red,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // SUBMIT
            Button(
                onClick = {
                    val isAllBlank = title.isBlank() && description.isBlank() && location.isBlank()
                    titleError = if (title.isBlank()) "Enter a report title." else ""
                    descError = if (description.isBlank()) "Describe the problem." else ""
                    locationError = if (location.isBlank()) "Enter the area or district." else ""

                    error = if (isAllBlank || titleError.isNotBlank() || descError.isNotBlank() || locationError.isNotBlank()) {
                        "Complete the required fields before submitting."
                    } else {
                        ""
                    }

                    if (titleError.isBlank() && descError.isBlank() && locationError.isBlank()) {
                        val currentUser = ReportStore.user
                        val online = isOnline(ctx)

                        val finalLocation = location.trim()
                        val finalLat = lat
                        val finalLng = lng

                        val newReport = Report(
                            id = ReportStore.nextId(),
                            title = title.trim(),
                            category = selected,
                            description = description.trim(),
                            photoPath = photoPath,
                            location = finalLocation,
                            landmark = landmark.trim(),
                            latitude = finalLat,
                            longitude = finalLng,
                            status = if (online) {
                                ReportStatus.SUBMITTED
                            } else {
                                ReportStatus.PENDING
                            },
                            citizenName = currentUser.name,
                            citizenEmail = currentUser.email
                        )

                        if (online) {
                            submitting = true
                            error = ""

                            try {
                                ReportStore.addReport(
                                    newReport,
                                    photoPath,
                                    onFailure = { message ->
                                        submitting = false
                                        if (ReportStore.find(newReport.id)?.status == ReportStatus.PENDING) {
                                            Toast.makeText(
                                                ctx,
                                                "Connection lost. Your report is saved and will sync when you reconnect.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onSubmit()
                                        } else {
                                            error = message.ifBlank {
                                                "Report submission failed. Please try again."
                                            }
                                        }
                                    },
                                    onComplete = {
                                            submitting = false
                                            val queued = ReportStore.find(newReport.id)?.status == ReportStatus.PENDING

                                            if (!queued) {
                                                ReportStore.notify(
                                                    currentUser.email,
                                                    "Report \"${newReport.title}\" submitted successfully."
                                                )
                                            }

                                            Toast.makeText(
                                                ctx,
                                                if (queued) "Report saved on this phone. Sign in while connected to send it." else "Report submitted successfully.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            onSubmit()
                                        }
                                    )
                                } catch (e: Exception) {
                                    submitting = false

                                    error = e.localizedMessage
                                        ?: "An unexpected error occurred."
                                }
                            } else {
                                ReportStore.addPending(newReport)

                                Toast.makeText(
                                    ctx,
                                    "Report saved offline. It will sync when internet is available.",
                                    Toast.LENGTH_LONG
                                ).show()

                                onSubmit()
                            }
                        }
                },
                enabled = !submitting && !locating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                )
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(10.dp))

                    Text("Submitting...")
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Submit Report",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel")
            }

            Spacer(Modifier.height(22.dp))
        } // Closes scrollable Column
    } // Closes outer Column
} // Closes ReportFormScreen

/* ---------------------------------------------------------
   TRACK REPORTS SCREEN
   --------------------------------------------------------- */

@Composable
fun TrackReportsScreen(
    nav: NavHostController,
    onDetails: (String) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var expanded by remember { mutableStateOf(false) }

    val filters = listOf(
        "All",
        "Pending",
        "Received",
        "In Progress",
        "Resolved"
    )

    val all = ReportStore.myReports()

    val list = all.filter {
        when (filter) {
            "Pending" ->
                it.status == ReportStatus.PENDING ||
                        it.status == ReportStatus.SUBMITTED

            "Received" ->
                it.status == ReportStatus.RECEIVED

            "In Progress" ->
                it.status == ReportStatus.IN_PROGRESS

            "Resolved" ->
                it.status == ReportStatus.RESOLVED

            else -> true
        }
    }

    AppScaffold(nav) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            Surface(
                color = BackgroundLight,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(16.dp)) {
                    Header(
                        "Track Reports",
                        "${all.size} total",
                        null
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filter Status: $filter",
                                fontWeight = FontWeight.Medium
                            )

                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        filters.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        item,
                                        fontWeight = if (filter == item) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        }
                                    )
                                },
                                onClick = {
                                    filter = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (list.isEmpty()) {
                    EmptyState(
                        "Nothing here",
                        "No reports match this filter.",
                        onClick = {}
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(list) { report ->
                            ReportCard(
                                report = report,
                                onClick = { onDetails(report.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
