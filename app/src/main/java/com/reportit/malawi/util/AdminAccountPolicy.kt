package com.reportit.malawi.util

import com.reportit.malawi.model.Role

/** Pure validation for the administrator-managed staff account workflow. */
object AdminAccountPolicy {
    enum class Action { CHANGE_ACCESS, REMOVE }

    const val ADMIN_MANAGER_EMAIL = "adminofficer@reportitmalawi.mw"

    private val malawiEmail = Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)*\\.mw$", RegexOption.IGNORE_CASE)

    fun isAdministratorManager(role: Role, email: String): Boolean =
        role == Role.ADMIN && email.trim().equals(ADMIN_MANAGER_EMAIL, ignoreCase = true)

    fun canHandleReports(role: Role): Boolean = role == Role.OFFICER

    fun validateCreate(managerRole: Role, managerEmail: String, name: String, email: String): String? {
        if (!isAdministratorManager(managerRole, managerEmail)) {
            return "Only the administrator manager can add officers."
        }
        if (name.trim().length < 2) return "Enter the administrator's full name."
        if (!malawiEmail.matches(email.trim())) {
            return "Enter a valid email address whose domain ends in .mw."
        }
        return null
    }

    fun validateManagement(
        managerRole: Role,
        managerEmail: String,
        targetEmail: String,
        action: Action
    ): String? {
        if (!isAdministratorManager(managerRole, managerEmail)) {
            return when (action) {
                Action.CHANGE_ACCESS -> "Only the administrator manager can modify officer accounts."
                Action.REMOVE -> "Only the administrator manager can remove officer accounts."
            }
        }
        if (!malawiEmail.matches(targetEmail.trim())) {
            return "Enter a valid staff email address whose domain ends in .mw."
        }
        if (targetEmail.trim().equals(managerEmail.trim(), ignoreCase = true)) {
            return when (action) {
                Action.CHANGE_ACCESS -> "You cannot disable your own active account."
                Action.REMOVE -> "You cannot remove your own account."
            }
        }
        return null
    }
}
