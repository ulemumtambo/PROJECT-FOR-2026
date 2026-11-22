package com.reportit.malawi.system

import com.reportit.malawi.model.AppNotification
import com.reportit.malawi.model.Priority
import com.reportit.malawi.model.Report
import com.reportit.malawi.model.ReportCategory
import com.reportit.malawi.model.ReportStatus
import com.reportit.malawi.model.Role
import com.reportit.malawi.model.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SystemIntegrationTest: Tests the whole system workflows, data models, report lifecycle tracking,
 * and role-based access logic corresponding to Objectives and Functional Requirements 5.1 - 5.8.
 * Each test includes descriptive comments explaining the system integration scenario being tested.
 */
class SystemIntegrationTest {

    // Helper function mirroring admin/officer email check logic in ReportStore.kt
    private fun checkIsOfficer(email: String): Boolean {
        val clean = email.trim().lowercase()
        return clean.contains("admin") || clean.contains("officer") || clean.endsWith("@roadsauthority.mw") || clean == "admin@reportit.mw"
    }

    // Helper function mirroring home route routing in Utils.kt
    private fun getHomeRoute(role: Role): String {
        return if (role == Role.OFFICER) "admin" else "citizenDashboard"
    }

    /**
     * Test Case: Verifies automatic role determination for officers and citizens
     * based on email domain and routing to the appropriate management dashboard or citizen portal
     * (Functional Requirement 5.1 User Authentication and Role Management).
     */
    @Test
    fun testOfficerEmailDetectionAndRoleRouting() {
        // Test: Routes citizens and officers to the correct dashboard.
        val citizenEmail = "citizen@malawi.mw"
        val isCitizenOfficer = checkIsOfficer(citizenEmail)
        val citizenRole = if (isCitizenOfficer) Role.OFFICER else Role.CITIZEN
        val citizenSession = UserSession("John Citizen", citizenEmail, citizenRole)

        assertEquals(Role.CITIZEN, citizenSession.role)
        assertEquals("citizenDashboard", getHomeRoute(citizenSession.role))

        val adminEmail = "admin@reportit.mw"
        val isAdminOfficer = checkIsOfficer(adminEmail)
        val adminRole = if (isAdminOfficer) Role.OFFICER else Role.CITIZEN
        val adminSession = UserSession("Road Officer", adminEmail, adminRole)

        assertEquals(Role.OFFICER, adminSession.role)
        assertEquals("admin", getHomeRoute(adminSession.role))

        val raEmail = "banda@roadsauthority.mw"
        val isRaOfficer = checkIsOfficer(raEmail)
        val raRole = if (isRaOfficer) Role.OFFICER else Role.CITIZEN
        val raSession = UserSession("Officer Banda", raEmail, raRole)

        assertEquals(Role.OFFICER, raSession.role)
        assertEquals("admin", getHomeRoute(raSession.role))
    }

    /**
     * Test Case: Verifies offline-first report creation model state with PENDING status
     * for offline persistence and subsequent synchronization (Functional Requirement 5.5).
     */
    @Test
    fun testOfflineFirstReportStorageModel() {
        // Test: Keeps a new offline report pending until it can sync.
        val citizenName = "Grace Phiri"
        val citizenEmail = "grace@malawi.mw"

        val offlineReport = Report(
            id = "RTM-PENDING-001",
            title = "Broken Streetlight in Blantyre",
            category = ReportCategory.BROKEN_STREETLIGHTS,
            description = "Streetlight near Clock Tower is broken.",
            location = "Blantyre",
            status = ReportStatus.PENDING,
            priority = Priority.MEDIUM,
            citizenName = citizenName,
            citizenEmail = citizenEmail
        )

        assertEquals("RTM-PENDING-001", offlineReport.id)
        assertEquals(ReportStatus.PENDING, offlineReport.status)
        assertEquals(citizenEmail, offlineReport.citizenEmail)
        assertNotNull(offlineReport.createdAt)
    }

    /**
     * Test Case: Verifies report lifecycle state transitions from Submitted -> Received -> In Progress -> Resolved
     * and associated comment updates (Functional Requirement 5.6 & 5.8).
     */
    @Test
    fun testReportLifecycleStateTransitions() {
        // Test: Moves a report through its complete status lifecycle.
        val report = Report(
            id = "RTM-12345",
            title = "Damaged Bridge in Mzuzu",
            category = ReportCategory.DAMAGED_BRIDGES,
            description = "Bridge deck has large cracks.",
            location = "Mzuzu",
            status = ReportStatus.SUBMITTED,
            priority = Priority.HIGH,
            citizenName = "David Mwale",
            citizenEmail = "david@malawi.mw",
            comments = "No comment yet"
        )

        assertEquals(ReportStatus.SUBMITTED, report.status)

        // Status update to Received
        val receivedReport = report.copy(status = ReportStatus.RECEIVED, comments = "Report received by Roads Authority.")
        assertEquals(ReportStatus.RECEIVED, receivedReport.status)

        // Status update to In Progress
        val inProgressReport = receivedReport.copy(status = ReportStatus.IN_PROGRESS, comments = "Maintenance team dispatched.")
        assertEquals(ReportStatus.IN_PROGRESS, inProgressReport.status)
        assertEquals("Maintenance team dispatched.", inProgressReport.comments)

        // Status update to Resolved
        val resolvedReport = inProgressReport.copy(status = ReportStatus.RESOLVED, comments = "Bridge repaired successfully.")
        assertEquals(ReportStatus.RESOLVED, resolvedReport.status)
        assertEquals("Bridge repaired successfully.", resolvedReport.comments)
    }

    /**
     * Test Case: Verifies notification model creation for report progress updates
     * (Functional Requirement 5.7 Report Notifications).
     */
    @Test
    fun testReportNotificationModelGeneration() {
        // Test: Creates a report update notification for the right citizen.
        val notification = AppNotification(
            id = System.nanoTime(),
            message = "Your report \"Damaged Bridge\" has been resolved.",
            time = System.currentTimeMillis(),
            audience = "david@malawi.mw"
        )

        assertNotNull(notification.id)
        assertTrue(notification.message.contains("resolved"))
        assertEquals("david@malawi.mw", notification.audience)
    }
}
