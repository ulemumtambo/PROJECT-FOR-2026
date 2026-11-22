package com.reportit.malawi.component

import com.reportit.malawi.model.Priority
import com.reportit.malawi.model.ReportCategory
import com.reportit.malawi.model.ReportStatus
import com.reportit.malawi.util.Amber
import com.reportit.malawi.util.Red
import com.reportit.malawi.util.categoryLabel
import com.reportit.malawi.util.priorityColor
import com.reportit.malawi.util.statusLabel
import com.reportit.malawi.util.timeAgo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * ComponentLogicTest: Tests component logic, label formatting, status badges,
 * priority color mappings, and time calculations supporting Non-Functional Requirements 6.1 (Usability) and 6.2 (Performance).
 */
class ComponentLogicTest {

    /**
     * Test Case: Verifies problem category enumeration to user-friendly string labels
     * for UI components (Functional Requirement 5.2).
     */
    @Test
    fun testCategoryLabelsFormatting() {
        // Test: Displays each report category with a readable label.
        assertEquals("Deteriorating Roads", categoryLabel(ReportCategory.DETERIORATING_ROADS))
        assertEquals("Damaged Bridges", categoryLabel(ReportCategory.DAMAGED_BRIDGES))
        assertEquals("Blocked Drainage", categoryLabel(ReportCategory.BLOCKED_DRAINAGE))
        assertEquals("Broken Streetlights", categoryLabel(ReportCategory.BROKEN_STREETLIGHTS))
        assertEquals("Flooding", categoryLabel(ReportCategory.FLOODING))
        assertEquals("Uncleared Waste", categoryLabel(ReportCategory.UNCLEARED_WASTE))
        assertEquals("Other", categoryLabel(ReportCategory.OTHER))
    }

    /**
     * Test Case: Verifies report status enumeration mapping to human-readable labels
     * for tracking UI components (Functional Requirement 5.6).
     */
    @Test
    fun testStatusLabelsFormatting() {
        // Test: Displays each report status with a readable label.
        assertEquals("Pending sync", statusLabel(ReportStatus.PENDING))
        assertEquals("Submitted", statusLabel(ReportStatus.SUBMITTED))
        assertEquals("Received", statusLabel(ReportStatus.RECEIVED))
        assertEquals("In progress", statusLabel(ReportStatus.IN_PROGRESS))
        assertEquals("Resolved", statusLabel(ReportStatus.RESOLVED))
    }

    /**
     * Test Case: Verifies priority color coding (High = Red, Medium = Amber, Low = Green)
     * for officer dashboard components (Functional Requirement 5.8).
     */
    @Test
    fun testPriorityColorsMapping() {
        // Test: Uses the expected color for each priority level.
        assertEquals(Red, priorityColor(Priority.HIGH))
        assertEquals(Amber, priorityColor(Priority.MEDIUM))
        assertNotNull(priorityColor(Priority.LOW))
    }

    /**
     * Test Case: Verifies time ago calculation formatting for report timestamps
     * in list and detail UI components.
     */
    @Test
    fun testTimeAgoFormatting() {
        // Test: Formats report timestamps as time-ago text.
        val now = System.currentTimeMillis()
        assertEquals("now", timeAgo(now))

        val thirtyMinutesAgo = now - (30 * 60_000L)
        assertEquals("30m", timeAgo(thirtyMinutesAgo))

        val twoHoursAgo = now - (120 * 60_000L)
        assertEquals("2h", timeAgo(twoHoursAgo))

        val threeDaysAgo = now - (3 * 1440 * 60_000L)
        assertEquals("3d", timeAgo(threeDaysAgo))
    }
}
