package com.reportit.malawi.model

/**
 * Models: I created these core data models and enumerations to represent application state,
 * user roles, report categories, status lifecycles, priorities, and notifications across ReportIt Malawi.
 */

// Admins manage staff accounts; officers handle reports and dashboard work.
enum class Role { CITIZEN, OFFICER, ADMIN }

// Report status lifecycle states
enum class ReportStatus { PENDING, SUBMITTED, RECEIVED, IN_PROGRESS, RESOLVED }

// Priority levels assigned to reports
enum class Priority { LOW, MEDIUM, HIGH }

// Categories of road infrastructure problems reported in Malawi
enum class ReportCategory {
    DETERIORATING_ROADS, DAMAGED_BRIDGES, BLOCKED_DRAINAGE,
    BROKEN_STREETLIGHTS, FLOODING, UNCLEARED_WASTE, OTHER
}

/**
 * I created UserSession to represent the active user's credentials and role.
 */
data class UserSession(val name: String, val email: String, val role: Role)

/**
 * I created Report data model to store problem details, geographic coordinates,
 * photos, lifecycle status, and Roads Authority officer comments.
 */
data class Report(
    val id: String,
    val title: String,
    val category: ReportCategory,
    val description: String,
    val imageRes: Int? = null,
    val photoPath: String? = null,      // Local file path or cloud URL
    val location: String,
    val landmark: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: ReportStatus,
    val priority: Priority = Priority.MEDIUM,
    val createdAt: Long = System.currentTimeMillis(),
    val citizenName: String,
    val citizenEmail: String,
    val comments: String = "No comment yet"
)

/**
 * I created AppNotification to represent real-time status alerts for citizens and officers.
 */
data class AppNotification(
    val id: Long,
    val message: String,
    val time: Long,
    val audience: String,   // Target citizen email or "OFFICERS"
    val reportId: String? = null,
    val read: Boolean = false
)
