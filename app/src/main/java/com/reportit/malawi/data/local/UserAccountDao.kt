package com.reportit.malawi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * UserAccountDao: Room Data Access Object for local user account operations.
 */
@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email)")
    suspend fun getAccountByEmail(email: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAccount(account: UserAccountEntity)

    @Query("UPDATE user_accounts SET passwordHash = :newPasswordHash WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePassword(email: String, newPasswordHash: String): Int

    @Query("UPDATE user_accounts SET name = :name WHERE LOWER(email) = LOWER(:email)")
    suspend fun updateName(email: String, name: String): Int

    @Query("UPDATE user_accounts SET isActive = :isActive WHERE LOWER(email) = LOWER(:email)")
    suspend fun updateActiveStatus(email: String, isActive: Boolean): Int

    @Query("DELETE FROM user_accounts WHERE LOWER(email) = LOWER(:email)")
    suspend fun deleteAccount(email: String): Int

    @Query("SELECT * FROM user_accounts")
    suspend fun getAllAccounts(): List<UserAccountEntity>
}
