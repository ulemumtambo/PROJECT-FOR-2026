package com.reportit.malawi.util

import com.reportit.malawi.model.Priority
import com.reportit.malawi.model.ReportCategory
import com.reportit.malawi.model.ReportStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * UtilsTest: Unit tests for formatting and utility functions in ReportIt Malawi.
 * Verifies category labels, status labels, priority color mapping, and time formatting
 * supporting Non-Functional Requirement 6.1 (Usability) and Functional Requirements 5.2, 5.6, 5.8.
 */
class UtilsTest {

    /**
     * Test Case: Verifies mapping of ReportCategory enums to human-readable strings.
     * What we are testing: Ensures categoryLabel() correctly translates technical category enums
     * (e.g. DETERIORATING_ROADS -> "Deteriorating Roads") for UI display (Functional Requirement 5.2).
     */
    @Test
    fun testCategoryLabelMapping() {
        // Test: Converts report categories into readable labels.
        assertEquals("Deteriorating Roads", categoryLabel(ReportCategory.DETERIORATING_ROADS))
        assertEquals("Damaged Bridges", categoryLabel(ReportCategory.DAMAGED_BRIDGES))
        assertEquals("Blocked Drainage", categoryLabel(ReportCategory.BLOCKED_DRAINAGE))
        assertEquals("Broken Streetlights", categoryLabel(ReportCategory.BROKEN_STREETLIGHTS))
        assertEquals("Flooding", categoryLabel(ReportCategory.FLOODING))
        assertEquals("Uncleared Waste", categoryLabel(ReportCategory.UNCLEARED_WASTE))
        assertEquals("Other", categoryLabel(ReportCategory.OTHER))
    }

    /**
     * Test Case: Verifies mapping of ReportStatus enums to tracking labels.
     * What we are testing: Ensures statusLabel() formats PENDING, SUBMITTED, RECEIVED, IN_PROGRESS,
     * and RESOLVED into clear status strings for report tracking (Functional Requirement 5.6).
     */
    @Test
    fun testStatusLabelMapping() {
        // Test: Converts report statuses into readable labels.
        assertEquals("Pending sync", statusLabel(ReportStatus.PENDING))
        assertEquals("Submitted", statusLabel(ReportStatus.SUBMITTED))
        assertEquals("Received", statusLabel(ReportStatus.RECEIVED))
        assertEquals("In progress", statusLabel(ReportStatus.IN_PROGRESS))
        assertEquals("Resolved", statusLabel(ReportStatus.RESOLVED))
    }

    /**
     * Test Case: Verifies priority color mappings for officer dashboard visualization.
     * What we are testing: Ensures priorityColor() maps High priority to Red and Medium priority to Amber
     * for visual prioritization in officer management (Functional Requirement 5.8).
     */
    @Test
    fun testPriorityColorNotNull() {
        // Test: Assigns colors for report priority levels.
        assertEquals(Red, priorityColor(Priority.HIGH))
        assertEquals(Amber, priorityColor(Priority.MEDIUM))
        assertNotNull(priorityColor(Priority.LOW))
    }

    /**
     * Test Case: Verifies time ago calculation formatting for report timestamps.
     * What we are testing: Ensures timeAgo() correctly converts current timestamps into relative strings
     * like "now" for enhanced usability (Non-Functional Requirement 6.1 Usability).
     */
    @Test
    fun testTimeAgoNow() {
        // Test: Shows a current timestamp as "now".
        val now = System.currentTimeMillis()
        assertEquals("now", timeAgo(now))
    }
}
