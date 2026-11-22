package com.reportit.malawi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserAccountEntity: Represents a persisted user account in the local Room database
 * to support secure offline authentication and account validation.
 */
@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val name: String,
    val role: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
