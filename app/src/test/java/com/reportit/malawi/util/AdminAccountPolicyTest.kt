package com.reportit.malawi.util

import com.reportit.malawi.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAccountPolicyTest {

    @Test
    fun onlyAdministratorManagerCanCreateStaffAccounts() {
        // Test: Only the administrator manager can add staff accounts.
        assertEquals(
            "Only the administrator manager can add officers.",
            AdminAccountPolicy.validateCreate(Role.OFFICER, "officer@agency.mw", "Staff Member", "staff@agency.mw")
        )
        assertEquals(
            "Only the administrator manager can add officers.",
            AdminAccountPolicy.validateCreate(Role.CITIZEN, "citizen@agency.mw", "Staff Member", "staff@agency.mw")
        )
        assertEquals(
            "Only the administrator manager can add officers.",
            AdminAccountPolicy.validateCreate(Role.ADMIN, "another-admin@agency.mw", "Staff Member", "staff@agency.mw")
        )
    }

    @Test
    fun newStaffAccountRequiresNameAndMalawiEmail() {
        // Test: Rejects a short name and emails that do not end in .mw.
        assertEquals(
            "Enter the administrator's full name.",
            AdminAccountPolicy.validateCreate(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL, " A ", "staff@agency.mw"
            )
        )
        assertEquals(
            "Enter a valid email address whose domain ends in .mw.",
            AdminAccountPolicy.validateCreate(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL, "Staff Member", "staff@example.com"
            )
        )
        assertEquals(
            "Enter a valid email address whose domain ends in .mw.",
            AdminAccountPolicy.validateCreate(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL, "Staff Member", "staff@@agency.mw"
            )
        )
    }

    @Test
    fun administratorManagerCanCreateStaffWithMixedCaseMwDomain() {
        // Test: Accepts a valid staff account with a case-insensitive .mw domain.
        assertNull(
            AdminAccountPolicy.validateCreate(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL, " Staff Member ", "staff@agency.MW"
            )
        )
    }

    @Test
    fun addedOfficerCannotChangeOrRemoveStaffAccounts() {
        // Test: Added officers cannot change or remove staff accounts.
        assertEquals(
            "Only the administrator manager can modify officer accounts.",
            AdminAccountPolicy.validateManagement(
                Role.OFFICER, "manager@agency.mw", "staff@agency.mw", AdminAccountPolicy.Action.CHANGE_ACCESS
            )
        )
        assertEquals(
            "Only the administrator manager can remove officer accounts.",
            AdminAccountPolicy.validateManagement(
                Role.OFFICER, "manager@agency.mw", "staff@agency.mw", AdminAccountPolicy.Action.REMOVE
            )
        )
    }

    @Test
    fun managerCannotDisableOrRemoveTheirOwnAccount() {
        // Test: The manager cannot disable or remove their own account.
        assertEquals(
            "You cannot disable your own active account.",
            AdminAccountPolicy.validateManagement(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL,
                " ADMINOFFICER@REPORTITMALAWI.MW ", AdminAccountPolicy.Action.CHANGE_ACCESS
            )
        )
        assertEquals(
            "You cannot remove your own account.",
            AdminAccountPolicy.validateManagement(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL,
                AdminAccountPolicy.ADMIN_MANAGER_EMAIL, AdminAccountPolicy.Action.REMOVE
            )
        )
    }

    @Test
    fun managerCanChangeOrRemoveAnotherStaffAccount() {
        // Test: The manager can change or remove another staff account.
        assertNull(
            AdminAccountPolicy.validateManagement(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL,
                "staff@agency.mw", AdminAccountPolicy.Action.CHANGE_ACCESS
            )
        )
        assertNull(
            AdminAccountPolicy.validateManagement(
                Role.ADMIN, AdminAccountPolicy.ADMIN_MANAGER_EMAIL,
                "staff@agency.mw", AdminAccountPolicy.Action.REMOVE
            )
        )
    }

    @Test
    fun onlyOfficerRoleCanHandleReports() {
        // Test: Only officers can handle reports; the manager account is limited to staff access.
        assertFalse(AdminAccountPolicy.canHandleReports(Role.ADMIN))
        assertTrue(AdminAccountPolicy.canHandleReports(Role.OFFICER))
        assertFalse(AdminAccountPolicy.canHandleReports(Role.CITIZEN))
    }
}
