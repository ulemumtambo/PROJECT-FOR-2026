package com.reportit.malawi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ReportEntity: Represents an infrastructure problem report stored in the local Room database
 * for offline persistence and automatic synchronization when online.
 */
@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val description: String,
    val photoPath: String?,
    val location: String,
    val landmark: String,
    val latitude: Double?,
    val longitude: Double?,
    val status: String,
    val priority: String,
    val createdAt: Long,
    val citizenName: String,
    val citizenEmail: String,
    val comments: String
)
