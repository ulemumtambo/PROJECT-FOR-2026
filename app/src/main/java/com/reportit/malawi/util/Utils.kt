package com.reportit.malawi.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.reportit.malawi.data.ReportStore
import com.reportit.malawi.data.local.ReportEntity
import com.reportit.malawi.model.Priority
import com.reportit.malawi.model.Report
import com.reportit.malawi.model.ReportCategory
import com.reportit.malawi.model.ReportStatus
import com.reportit.malawi.model.Role
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * Utils: I created this utility module to provide application-wide helper functions,
 * network connectivity checks, gallery image file copying, GPS location fetching,
 * timestamp formatting, password hashing, and UI styling utilities.
 */

val Amber = Color(0xFFFFB300)
val Red = Color(0xFFE53935)
val Blue = Color(0xFF1976D2)

// Password hashing helper using SHA-256
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun verifyPassword(password: String, storedHash: String): Boolean {
    return hashPassword(password) == storedHash
}

// Model & Entity Mappers
fun Report.toEntity(): ReportEntity = ReportEntity(
    id = id,
    title = title,
    category = category.name,
    description = description,
    photoPath = photoPath,
    location = location,
    landmark = landmark,
    latitude = latitude,
    longitude = longitude,
    status = status.name,
    priority = priority.name,
    createdAt = createdAt,
    citizenName = citizenName,
    citizenEmail = citizenEmail,
    comments = comments
)

fun ReportEntity.toReport(): Report = Report(
    id = id,
    title = title,
    category = runCatching { ReportCategory.valueOf(category) }.getOrDefault(ReportCategory.OTHER),
    description = description,
    photoPath = photoPath,
    location = location,
    landmark = landmark,
    latitude = latitude,
    longitude = longitude,
    status = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.SUBMITTED),
    priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIUM),
    createdAt = createdAt,
    citizenName = citizenName,
    citizenEmail = citizenEmail,
    comments = comments
)

// I check if the device currently has active internet connectivity
fun isOnline(ctx: Context): Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

// I copy a photo selected from the gallery into the app's local private storage
fun copyUriToPhoto(ctx: Context, uri: Uri): String? = try {
    val f = File(ctx.filesDir, "report_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    f.absolutePath
} catch (e: Exception) { null }

// I check if location permissions are granted
fun hasLocationPermission(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

// I fetch the current GPS location (checking last known location first, then requesting a fresh fix)
@SuppressLint("MissingPermission")
fun fetchLocation(ctx: Context, onResult: (Location?) -> Unit) {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { try { lm.isProviderEnabled(it) } catch (e: Exception) { false } }
    if (providers.isEmpty()) { onResult(null); return }

    val last = providers.mapNotNull { try { lm.getLastKnownLocation(it) } catch (e: SecurityException) { null } }
        .maxByOrNull { it.time }
    if (last != null) { onResult(last); return }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lm.getCurrentLocation(providers.first(), null, ctx.mainExecutor) { onResult(it) }
        } else {
            var done = false
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!done) { done = true; onResult(location) }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(providers.first(), listener, Looper.getMainLooper())
            // give up after 20 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                if (!done) { done = true; lm.removeUpdates(listener); onResult(null) }
            }, 20_000)
        }
    } catch (e: Exception) {
        onResult(null)
    }
}

// I format timestamps into human-readable relative time strings
fun timeAgo(ts: Long): String {
    val mins = (System.currentTimeMillis() - ts) / 60_000
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        mins < 1440 -> "${mins / 60}h"
        else -> "${mins / 1440}d"
    }
}

// Category display label helper
fun categoryLabel(c: ReportCategory) = when (c) {
    ReportCategory.DETERIORATING_ROADS -> "Deteriorating Roads"
    ReportCategory.DAMAGED_BRIDGES -> "Damaged Bridges"
    ReportCategory.BLOCKED_DRAINAGE -> "Blocked Drainage"
    ReportCategory.BROKEN_STREETLIGHTS -> "Broken Streetlights"
    ReportCategory.FLOODING -> "Flooding"
    ReportCategory.UNCLEARED_WASTE -> "Uncleared Waste"
    ReportCategory.OTHER -> "Other"
}

// Status display label helper
fun statusLabel(s: ReportStatus) = when (s) {
    ReportStatus.PENDING -> "Pending sync"
    else -> s.name.replace("_", " ").lowercase(Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

// Priority color helper
fun priorityColor(p: Priority) = when (p) {
    Priority.HIGH -> Red
    Priority.MEDIUM -> Amber
    Priority.LOW -> Color(0xFF18A66B)
}

// Navigation destination helper based on user role
fun homeRoute() = if (ReportStore.user.role == Role.CITIZEN) "citizenDashboard" else "admin"
