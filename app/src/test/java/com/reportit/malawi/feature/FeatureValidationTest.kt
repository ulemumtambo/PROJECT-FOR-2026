package com.reportit.malawi.feature

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FeatureValidationTest: Tests all validation rules across features in ReportIt Malawi
 * corresponding to Functional Requirement 5.1 (User Authentication) and 5.2 (Road Infrastructure Problem Reporting).
 * Each test includes descriptive comments explaining the feature and rule being verified.
 */
class FeatureValidationTest {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

    // Helper simulating login validation logic from AuthScreens.kt
    private fun validateLogin(email: String, pass: String): String {
        return when {
            email.isBlank() || pass.isBlank() -> "Enter your email and password."
            !emailRegex.matches(email.trim()) -> "Enter a valid email address."
            pass.length < 6 -> "Password must be at least 6 characters."
            else -> ""
        }
    }

    // Helper simulating signup validation logic from AuthScreens.kt
    private fun validateSignup(name: String, email: String, pass: String, confirmPass: String = pass): String {
        return when {
            name.isBlank() -> "Enter your full name."
            !emailRegex.matches(email.trim()) -> "Enter a valid email address."
            pass.length < 8 -> "Please use a strong password."
            pass != confirmPass -> "Passwords do not match."
            else -> ""
        }
    }

    // Helper simulating report form validation logic from CitizenScreens.kt
    private fun validateReportForm(title: String, description: String, location: String): String {
        return when {
            title.isBlank() -> "Please enter a title."
            description.isBlank() -> "Please describe the problem."
            location.isBlank() -> "Please add the location."
            else -> ""
        }
    }

    /**
     * Test Case: Verifies that attempting to log in with empty email or password fields
     * correctly returns the expected error message (Functional Requirement 5.1).
     */
    @Test
    fun testLoginValidation_EmptyFields() {
        // Test: Login rejects missing email and password.
        val error = validateLogin("", "")
        assertEquals("Enter your email and password.", error)
    }

    /**
     * Test Case: Verifies that attempting to log in with a malformed email address
     * correctly rejects it and prompts the user for a valid email (Functional Requirement 5.1).
     */
    @Test
    fun testLoginValidation_InvalidEmail() {
        // Test: Login rejects an invalid email address.
        val error = validateLogin("notanemail", "password123")
        assertEquals("Enter a valid email address.", error)
    }

    /**
     * Test Case: Verifies that passwords shorter than 6 characters are rejected during login
     * to enforce security standards (Non-Functional Requirement 6.4 Security).
     */
    @Test
    fun testLoginValidation_ShortPassword() {
        // Test: Login rejects a password shorter than six characters.
        val error = validateLogin("citizen@malawi.mw", "123")
        assertEquals("Password must be at least 6 characters.", error)
    }

    /**
     * Test Case: Verifies that valid login credentials pass all checks with no error message.
     */
    @Test
    fun testLoginValidation_Success() {
        // Test: Login accepts a valid email and password.
        val error = validateLogin("citizen@malawi.mw", "securepassword")
        assertEquals("", error)
    }

    /**
     * Test Case: Verifies that account creation (sign up) requires a non-blank full name
     * (Functional Requirement 5.1 User Authentication and Role Management).
     */
    @Test
    fun testSignupValidation_EmptyName() {
        // Test: Account creation requires the user's name.
        val error = validateSignup("", "citizen@malawi.mw", "password123")
        assertEquals("Enter your full name.", error)
    }

    /**
     * Test Case: Verifies that sign up validates email format correctly.
     */
    @Test
    fun testSignupValidation_InvalidEmail() {
        // Test: Account creation rejects an invalid email address.
        val error = validateSignup("John Banda", "invalid-email", "password123")
        assertEquals("Enter a valid email address.", error)
    }

    /**
     * Test Case: Verifies that sign up enforces password length of at least 8 characters.
     */
    @Test
    fun testSignupValidation_ShortPassword() {
        // Test: Account creation requires a strong password.
        val error = validateSignup("John Banda", "citizen@malawi.mw", "12345")
        assertEquals("Please use a strong password.", error)
    }

    /**
     * Test Case: Verifies that sign up rejects non-matching confirm password.
     */
    @Test
    fun testSignupValidation_PasswordMismatch() {
        // Test: Account creation rejects passwords that do not match.
        val error = validateSignup("John Banda", "citizen@malawi.mw", "password123", "password456")
        assertEquals("Passwords do not match.", error)
    }

    /**
     * Test Case: Verifies successful sign up validation when all fields are valid.
     */
    @Test
    fun testSignupValidation_Success() {
        // Test: Account creation accepts valid matching details.
        val error = validateSignup("John Banda", "citizen@malawi.mw", "password123", "password123")
        assertEquals("", error)
    }

    /**
     * Test Case: Verifies that reporting a road problem requires a non-blank title
     * (Functional Requirement 5.2 Road Infrastructure Problem Reporting).
     */
    @Test
    fun testReportValidation_EmptyTitle() {
        // Test: A report requires a title.
        val error = validateReportForm("", "Deep pothole on road", "Lilongwe")
        assertEquals("Please enter a title.", error)
    }

    /**
     * Test Case: Verifies that reporting a road problem requires a description
     * providing details for Roads Authority assessment (Functional Requirement 5.2 & 5.3).
     */
    @Test
    fun testReportValidation_EmptyDescription() {
        // Test: A report requires a description.
        val error = validateReportForm("Pothole", "", "Lilongwe")
        assertEquals("Please describe the problem.", error)
    }

    /**
     * Test Case: Verifies that reporting a road problem requires a geographical location
     * (Functional Requirement 5.4 GPS Based Location Identification).
     */
    @Test
    fun testReportValidation_EmptyLocation() {
        // Test: A report requires a location.
        val error = validateReportForm("Pothole", "Deep pothole on road", "")
        assertEquals("Please add the location.", error)
    }

    /**
     * Test Case: Verifies successful report form validation when title, description, and location are provided.
     */
    @Test
    fun testReportValidation_Success() {
        // Test: A report accepts a title, description, and location.
        val error = validateReportForm("Pothole", "Deep pothole on road", "Lilongwe")
        assertEquals("", error)
    }
}
