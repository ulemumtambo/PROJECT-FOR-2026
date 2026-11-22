package com.reportit.malawi.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reportit.malawi.data.ReportStore
import com.reportit.malawi.ui.screens.admin.AdminDashboardScreen
import com.reportit.malawi.ui.screens.admin.AdminReportsScreen
import com.reportit.malawi.ui.screens.admin.UpdateStatusScreen
import com.reportit.malawi.ui.screens.auth.ForgotPasswordScreen
import com.reportit.malawi.ui.screens.auth.LoginScreen
import com.reportit.malawi.ui.screens.auth.SignUpScreen
import com.reportit.malawi.ui.screens.auth.SplashScreen
import com.reportit.malawi.ui.screens.citizen.CitizenDashboardScreen
import com.reportit.malawi.ui.screens.citizen.ReportFormScreen
import com.reportit.malawi.ui.screens.citizen.TrackReportsScreen
import com.reportit.malawi.ui.screens.common.NotificationsScreen
import com.reportit.malawi.ui.screens.common.ReportDetailsScreen
import com.reportit.malawi.ui.screens.common.SettingsScreen
import com.reportit.malawi.util.homeRoute

/**
 * ReportItMalawiApp: I created this root composable to manage navigation routes,
 * automatic network connectivity listeners, and background sync triggers across the entire application.
 */
@Composable
fun ReportItMalawiApp() {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    // I registered a network connectivity callback to automatically sync offline pending reports when internet is restored
    DisposableEffect(Unit) {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val handler = Handler(Looper.getMainLooper())
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.post {
                    if (ReportStore.autoSync && ReportStore.syncPending() > 0) {
                        Toast.makeText(ctx, "Pending reports syncing to cloud...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        try { cm.registerDefaultNetworkCallback(cb) } catch (_: Exception) { }
        onDispose { try { cm.unregisterNetworkCallback(cb) } catch (_: Exception) { } }
    }

    // Navigation Graph definition
    NavHost(navController = nav, startDestination = "splash") {

        composable("splash") {
            LaunchedEffect(ReportStore.sessionReady) {
                if (ReportStore.sessionReady) {
                    val destination = if (ReportStore.user.email.isNotBlank()) homeRoute() else "login"
                    nav.navigate(destination) { popUpTo("splash") { inclusive = true } }
                }
            }
            SplashScreen {
                if (ReportStore.sessionReady) {
                    val destination = if (ReportStore.user.email.isNotBlank()) homeRoute() else "login"
                    nav.navigate(destination) { popUpTo("splash") { inclusive = true } }
                }
            }
        }

        composable("login") {
            LoginScreen(
                onLogin = { email, password, onFail ->
                    ReportStore.login(email, password,
                        onSuccess = {
                            ReportStore.startListening()
                            nav.navigate(homeRoute()) { popUpTo("login") { inclusive = true } }
                        },
                        onFailure = onFail)
                },
                onSignUp = { nav.navigate("signup") },
                onForgot = { nav.navigate("forgot") }
            )
        }

        composable("signup") {
            SignUpScreen(
                onBack = { nav.popBackStack() },
                onCreated = { name, email, password, onFail ->
                    ReportStore.signUp(name, email, password,
                        onSuccess = {
                            ReportStore.startListening()
                            nav.navigate(homeRoute()) { popUpTo("login") { inclusive = true } }
                        },
                        onFailure = onFail)
                }
            )
        }

        composable("forgot") { ForgotPasswordScreen(onBack = { nav.popBackStack() }) }

        composable("citizenDashboard") {
            CitizenDashboardScreen(nav,
                onReport = { nav.navigate("report") },
                onDetails = { nav.navigate("details/$it") })
        }

        composable("report") {
            ReportFormScreen(
                onBack = { nav.popBackStack() },
                onSubmit = { nav.navigate("track") { popUpTo("citizenDashboard") } }
            )
        }

        composable("track") { TrackReportsScreen(nav, onDetails = { nav.navigate("details/$it") }) }

        composable("notifications") { NotificationsScreen(nav) }

        composable("settings") {
            SettingsScreen(nav, onLogout = {
                ReportStore.logout()
                nav.navigate("login") { popUpTo(0) }
            })
        }

        composable("admin") {
            AdminDashboardScreen(nav,
                onManage = { nav.navigate("adminReports") },
                onDetails = { nav.navigate("details/$it") })
        }

        composable("adminReports") {
            AdminReportsScreen(nav,
                onDetails = { nav.navigate("details/$it") },
                onUpdate = { nav.navigate("update/$it") })
        }

        composable("details/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            val id = it.arguments?.getString("id").orEmpty()
            ReportDetailsScreen(id, onBack = { nav.popBackStack() }, onUpdate = { nav.navigate("update/$id") })
        }

        composable("update/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            val id = it.arguments?.getString("id").orEmpty()
            UpdateStatusScreen(id, onBack = { nav.popBackStack() })
        }
    }
}
