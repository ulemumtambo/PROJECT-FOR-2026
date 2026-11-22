package com.reportit.malawi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * ReportDao: Room Data Access Object for local report persistence and offline queue operations.
 */
@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    suspend fun getAllReports(): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getReportById(id: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReport(report: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReports(reports: List<ReportEntity>)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReportById(id: String)

    @Query("DELETE FROM reports WHERE status = 'RESOLVED'")
    suspend fun deleteResolvedReports(): Int

    @Query("DELETE FROM reports")
    suspend fun deleteAllReports()
}
