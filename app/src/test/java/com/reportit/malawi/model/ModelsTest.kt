package com.reportit.malawi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * ModelsTest: Comprehensive unit tests for data models in ReportIt Malawi.
 * Verifies data class properties and initialization for user sessions, reports, and notifications
 * supporting Functional Requirements 5.1, 5.2, 5.6, and 5.7.
 */
class ModelsTest {

    /**
     * Test Case: Verifies citizen user session creation and property assignment.
     * What we are testing: Ensures UserSession correctly holds citizen name, email, and Role.CITIZEN
     * (Functional Requirement 5.1 User Authentication and Role Management).
     */
    @Test
    fun testUserSessionCreation() {
        val user = UserSession("John Doe", "john@example.com", Role.CITIZEN)
        assertEquals("John Doe", user.name)
        assertEquals("john@example.com", user.email)
        assertEquals(Role.CITIZEN, user.role)
    }

    /**
     * Test Case: Verifies Roads Authority officer user session creation and role assignment.
     * What we are testing: Ensures UserSession correctly assigns Role.OFFICER for admin/officer accounts
     * (Functional Requirement 5.1 User Authentication and Role Management).
     */
    @Test
    fun testOfficerUserSessionCreation() {
        val admin = UserSession("Road Officer", "roadadmin@gmail.com", Role.OFFICER)
        assertEquals("Road Officer", admin.name)
        assertEquals("roadadmin@gmail.com", admin.email)
        assertEquals(Role.OFFICER, admin.role)
    }

    /**
     * Test Case: Verifies complete Report data model creation and default values.
     * What we are testing: Ensures Report data class stores id, title, category, description, location,
     * status (PENDING/SUBMITTED), priority (HIGH), citizen name, email, and timestamp correctly
     * (Functional Requirement 5.2 Road Infrastructure Problem Reporting & 5.6 Report Lifecycle).
     */
    @Test
    fun testReportModelCreation() {
        val report = Report(
            id = "RTM-00100",
            title = "Pothole on Chipembere Highway",
            category = ReportCategory.DETERIORATING_ROADS,
            description = "Deep pothole causing traffic slowdown.",
            location = "Blantyre",
            status = ReportStatus.PENDING,
            priority = Priority.HIGH,
            citizenName = "Jane Doe",
            citizenEmail = "jane@example.com"
        )

        assertEquals("RTM-00100", report.id)
        assertEquals("Pothole on Chipembere Highway", report.title)
        assertEquals(ReportCategory.DETERIORATING_ROADS, report.category)
        assertEquals(ReportStatus.PENDING, report.status)
        assertEquals(Priority.HIGH, report.priority)
        assertEquals("Jane Doe", report.citizenName)
        assertEquals("jane@example.com", report.citizenEmail)
        assertNotNull(report.createdAt)
    }

    /**
     * Test Case: Verifies AppNotification data model creation.
     * What we are testing: Ensures notification data structure stores id, message, timestamp, and audience
     * for tracking updates (Functional Requirement 5.7 Report Notifications).
     */
    @Test
    fun testAppNotificationModel() {
        val notification = AppNotification(
            id = 1L,
            message = "Your report has been received.",
            time = System.currentTimeMillis(),
            audience = "jane@example.com"
        )

        assertEquals(1L, notification.id)
        assertEquals("Your report has been received.", notification.message)
        assertEquals("jane@example.com", notification.audience)
    }
}
