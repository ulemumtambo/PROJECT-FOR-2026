package com.reportit.malawi.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.reportit.malawi.data.local.AppDatabase
import com.reportit.malawi.data.local.UserAccountEntity
import com.reportit.malawi.model.AppNotification
import com.reportit.malawi.model.Priority
import com.reportit.malawi.model.Report
import com.reportit.malawi.model.ReportCategory
import com.reportit.malawi.model.ReportStatus
import com.reportit.malawi.model.Role
import com.reportit.malawi.model.UserSession
import com.reportit.malawi.util.hashPassword
import com.reportit.malawi.util.AdminAccountPolicy
import com.reportit.malawi.util.isOnline
import com.reportit.malawi.util.statusLabel
import com.reportit.malawi.util.toEntity
import com.reportit.malawi.util.toReport
import com.reportit.malawi.util.verifyPassword
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.random.Random

/**
 * ReportStore: Central data repository managing application state,
 * Firebase Authentication, Firestore real-time sync, Room database offline persistence,
 * secure offline authentication, and in-app notifications.
 */
object ReportStore {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var roomDb: AppDatabase? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    // Active user session
    var user by mutableStateOf(
        UserSession("", "", Role.CITIZEN)
    )
    var sessionReady by mutableStateOf(false)
        private set

    private val sessionPrefs by lazy {
        requireNotNull(appContext).getSharedPreferences("reportit_session", Context.MODE_PRIVATE)
    }

    // Global settings toggles
    var notificationsOn by mutableStateOf(true)
    var autoSync by mutableStateOf(true)

    // Reactive snapshot state lists for UI observation
    val reports: SnapshotStateList<Report> = mutableStateListOf()
    val notifications: SnapshotStateList<AppNotification> = mutableStateListOf()
    val adminAccounts: SnapshotStateList<UserAccountEntity> = mutableStateListOf()

    private var listener: ListenerRegistration? = null
    private val syncing = mutableSetOf<String>()
    private var appContext: Context? = null
    private val deletedNotificationIds = mutableSetOf<Long>()

    fun init(context: Context) {
        appContext = context.applicationContext
        roomDb = AppDatabase.getInstance(context.applicationContext)
        createNotificationChannel()

        // Restore the last signed-in account from local storage, including when offline.
        CoroutineScope(Dispatchers.IO).launch {
            val savedEmail = context.applicationContext
                .getSharedPreferences("reportit_session", Context.MODE_PRIVATE)
                .getString("email", null)
            val account = savedEmail?.let { roomDb?.userAccountDao()?.getAccountByEmail(it) }
            val firebaseUser = auth.currentUser
            val cloudProfile = if (
                account != null &&
                firebaseUser?.email.equals(savedEmail, ignoreCase = true) &&
                isOnline(context.applicationContext)
            ) {
                runCatching {
                    db.collection("users").document(firebaseUser!!.uid)
                        .get(com.google.firebase.firestore.Source.SERVER)
                        .await()
                }.getOrNull()
            } else {
                null
            }
            withContext(Dispatchers.Main) {
                if (account != null) {
                    val savedSession = context.applicationContext
                        .getSharedPreferences("reportit_session", Context.MODE_PRIVATE)
                    if (cloudProfile != null &&
                        (!cloudProfile.exists() || cloudProfile.getBoolean("isActive") == false)
                    ) {
                        auth.signOut()
                        savedSession.edit().remove("email").apply()
                        resetSession()
                    } else {
                        val cloudRole = cloudProfile?.getString("role")?.trim()?.uppercase()
                        val resolvedRole = when (cloudRole) {
                            "ADMIN" -> Role.ADMIN
                            "OFFICER" -> Role.OFFICER
                            "CITIZEN" -> Role.CITIZEN
                            else -> runCatching { Role.valueOf(account.role) }.getOrDefault(Role.CITIZEN)
                        }
                        val role = if (resolvedRole == Role.ADMIN &&
                            !AdminAccountPolicy.isAdministratorManager(resolvedRole, account.email)
                        ) Role.OFFICER else resolvedRole
                        user = UserSession(
                            cloudProfile?.getString("name")?.takeIf { it.isNotBlank() } ?: account.name,
                            account.email,
                            role
                        )
                        if (role != Role.valueOf(account.role)) {
                            CoroutineScope(Dispatchers.IO).launch {
                                roomDb?.userAccountDao()?.insertOrUpdateAccount(account.copy(role = role.name))
                            }
                        }
                        startListening()
                    }
                } else {
                    val firebaseEmail = firebaseUser?.email
                    if (!firebaseEmail.isNullOrBlank() && firebaseEmail.equals(savedEmail, ignoreCase = true)) {
                        user = UserSession(firebaseEmail.substringBefore("@"), firebaseEmail, Role.CITIZEN)
                        startListening()
                    }
                }
                sessionReady = true
            }
        }

        // Load persisted local reports from Room DB into state list
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localReports = roomDb?.reportDao()?.getAllReports()?.map { it.toReport() } ?: emptyList()
                withContext(Dispatchers.Main) {
                    if (user.role == Role.ADMIN) {
                        reports.clear()
                    } else if (localReports.isNotEmpty()) {
                        reports.clear()
                        reports.addAll(localReports)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun createNotificationChannel() {
        val ctx = appContext ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "reportit_alerts"
            val channelName = "ReportIt Malawi Alerts"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                description = "Real-time infrastructure report updates and emergency alerts"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ---------------------------------------------------------
    // AUTHENTICATION & SESSION MANAGEMENT
    // ---------------------------------------------------------

    /**
     * Login with online Firebase authentication and secure Room database offline fallback.
     * Only users who have created an account (stored locally or in Firebase) can log in.
     */
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() || password.isBlank()) {
            onFailure("Enter your email and password.")
            return
        }

        val ctx = appContext
        val online = ctx != null && isOnline(ctx)

        CoroutineScope(Dispatchers.IO).launch {
            val localAccount = roomDb?.userAccountDao()?.getAccountByEmail(cleanEmail)
            val localPasswordValid = localAccount != null && verifyPassword(password, localAccount.passwordHash)

            if (!online) {
                performOfflineLoginFallback(cleanEmail, password, localAccount, "No internet connection.", onSuccess, onFailure)
                return@launch
            }

            auth.signInWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid.isNullOrBlank()) {
                        auth.signOut()
                        if (localPasswordValid) {
                            performOfflineLoginFallback(cleanEmail, password, localAccount, null, onSuccess, onFailure)
                        } else {
                            onFailure("Could not identify your account.")
                        }
                        return@addOnSuccessListener
                    }

                    db.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener profileSuccess@{ document ->
                            val isActive = document.getBoolean("isActive") ?: true
                            if (!isActive) {
                                auth.signOut()
                                onFailure("This account has been disabled by an administrator.")
                                return@profileSuccess
                            }
                            if (!document.exists()) {
                                auth.signOut()
                                onFailure("This account has no ReportIt profile.")
                                return@profileSuccess
                            }
                            val roleValue = document.getString("role")?.trim()?.uppercase()
                            val name = document.getString("name")?.takeIf { it.isNotBlank() }
                                ?: cleanEmail.substringBefore("@")

                            val appRole = when (roleValue) {
                                "ADMIN" -> if (AdminAccountPolicy.isAdministratorManager(Role.ADMIN, cleanEmail)) Role.ADMIN else Role.OFFICER
                                "OFFICER" -> Role.OFFICER
                                "CITIZEN" -> Role.CITIZEN
                                else -> {
                                    auth.signOut()
                                    onFailure("This account has an invalid ReportIt role.")
                                    return@profileSuccess
                                }
                            }

                            setActiveUser(UserSession(name = name, email = cleanEmail, role = appRole))

                            // Persist user account and updated password hash to Room database
                            CoroutineScope(Dispatchers.IO).launch {
                                roomDb?.userAccountDao()?.insertOrUpdateAccount(
                                    UserAccountEntity(
                                        email = cleanEmail.lowercase(),
                                        name = name,
                                        role = appRole.name,
                                        passwordHash = hashPassword(password)
                                    )
                                )
                            }

                            startListening()
                            syncPending()
                            onSuccess()
                        }
                        .addOnFailureListener { error ->
                            val isNetworkError = error is FirebaseNetworkException ||
                                    error is IOException ||
                                    error.localizedMessage?.contains("network", ignoreCase = true) == true

                            if (localPasswordValid || isNetworkError) {
                                performOfflineLoginFallback(cleanEmail, password, localAccount, error.localizedMessage, onSuccess, onFailure)
                            } else {
                                onFailure(error.localizedMessage ?: "Could not load your account profile.")
                            }
                        }
                }
                .addOnFailureListener { error ->
                    val isNetworkError = error is FirebaseNetworkException ||
                            error is IOException ||
                            error.localizedMessage?.contains("network", ignoreCase = true) == true ||
                            error.localizedMessage?.contains("connect", ignoreCase = true) == true

                    if (localPasswordValid || isNetworkError) {
                        performOfflineLoginFallback(cleanEmail, password, localAccount, error.localizedMessage, onSuccess, onFailure)
                    } else {
                        onFailure(error.localizedMessage ?: "Email or password is incorrect.")
                    }
                }
        }
    }

    private fun performOfflineLoginFallback(
        cleanEmail: String,
        password: String,
        localAccount: UserAccountEntity?,
        onlineErrorMsg: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        scope.launch {
            if (localAccount == null) {
                onFailure(onlineErrorMsg ?: "No account found for this email. Only registered accounts can log in.")
                return@launch
            }

            if (!localAccount.isActive) {
                onFailure("This account has been disabled by an administrator.")
                return@launch
            }

            if (!verifyPassword(password, localAccount.passwordHash)) {
                onFailure("Incorrect password. Please enter your valid password.")
                return@launch
            }

            val storedRole = runCatching { Role.valueOf(localAccount.role) }.getOrDefault(Role.CITIZEN)
            val appRole = if (storedRole == Role.ADMIN &&
                !AdminAccountPolicy.isAdministratorManager(storedRole, localAccount.email)
            ) Role.OFFICER else storedRole
            setActiveUser(UserSession(localAccount.name, localAccount.email, appRole))
            startListening()
            onSuccess()
        }
    }

    /** Public accounts must be created in Firebase, so registration requires internet. */
    fun signUp(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || password.isBlank()) {
            onFailure("Complete all required fields.")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onFailure("Enter a valid email address.")
            return
        }

        if (password.length < 8) {
            onFailure("Please use a strong password (at least 8 characters).")
            return
        }

        val ctx = appContext
        val online = ctx != null && isOnline(ctx)
        if (!online) {
            onFailure("Connect to the internet to create your account.")
            return
        }

        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid.isNullOrBlank()) {
                        auth.signOut()
                        onFailure("Could not create your account profile.")
                        return@addOnSuccessListener
                    }

                    val profile = hashMapOf(
                        "name" to cleanName,
                        "email" to cleanEmail,
                        "role" to "CITIZEN"
                    )

                    db.collection("users")
                        .document(uid)
                        .set(profile)
                        .addOnSuccessListener {
                            CoroutineScope(Dispatchers.IO).launch {
                                roomDb?.userAccountDao()?.insertOrUpdateAccount(
                                    UserAccountEntity(cleanEmail.lowercase(), cleanName, Role.CITIZEN.name, hashPassword(password))
                                )
                            }
                            setActiveUser(UserSession(cleanName, cleanEmail, Role.CITIZEN))
                            startListening()
                            onSuccess()
                        }
                        .addOnFailureListener {
                            CoroutineScope(Dispatchers.IO).launch {
                                roomDb?.userAccountDao()?.insertOrUpdateAccount(
                                    UserAccountEntity(cleanEmail.lowercase(), cleanName, Role.CITIZEN.name, hashPassword(password))
                                )
                            }
                            setActiveUser(UserSession(cleanName, cleanEmail, Role.CITIZEN))
                            startListening()
                            onSuccess()
                        }
                }
                .addOnFailureListener { error ->
                    if (error.localizedMessage?.contains("already in use") == true) {
                        onFailure("An account with this email already exists.")
                    } else {
                        onFailure(error.localizedMessage ?: "Could not create your account. Please try again.")
                    }
                }
    }

    /** Creates an administrator account through the role-checked Cloud Function. */
    fun createAdminAccount(
        name: String,
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        AdminAccountPolicy.validateCreate(user.role, user.email, cleanName, cleanEmail)?.let { error ->
            onFailure(error)
            return
        }

        val ctx = appContext
        if (ctx == null || !isOnline(ctx)) {
            onFailure("Connect to the internet to add an administrator.")
            return
        }

        scope.launch {
            try {
                FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("createAdminAccount")
                    .call(mapOf("name" to cleanName, "email" to cleanEmail))
                    .await()

                val setupEmailSent = runCatching {
                    auth.sendPasswordResetEmail(cleanEmail).await()
                }.isSuccess

                withContext(Dispatchers.IO) {
                    roomDb?.userAccountDao()?.insertOrUpdateAccount(
                        UserAccountEntity(
                            email = cleanEmail.lowercase(),
                            name = cleanName,
                            role = Role.OFFICER.name,
                            passwordHash = "",
                            isActive = true
                        )
                    )
                }
                loadAdminAccounts()

                onSuccess(
                    if (setupEmailSent) {
                        "Administrator added. Password setup email sent."
                    } else {
                        "Administrator added, but the setup email could not be sent. They can use Forgot password to request it."
                    }
                )
            } catch (error: Exception) {
                onFailure(error.localizedMessage ?: "Could not create the administrator account.")
            }
        }
    }

    /**
     * Firebase sends a verified reset link. The user chooses their new password through
     * Firebase's secure action flow; the local offline credential is invalidated until
     * the next successful online sign-in caches the new credential.
     */
    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onFailure("Enter a valid email address.")
            return
        }

        val ctx = appContext
        if (ctx == null || !isOnline(ctx)) {
            onFailure("Connect to the internet to reset your password.")
            return
        }

        auth.sendPasswordResetEmail(cleanEmail)
            .addOnSuccessListener {
                CoroutineScope(Dispatchers.IO).launch {
                    roomDb?.userAccountDao()?.updatePassword(cleanEmail, "")
                }
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.localizedMessage ?: "Could not send the password reset email.")
            }
    }

    /**
     * Update user's display name in Firestore, local state, and Room DB.
     */
    fun updateProfileName(
        newName: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanName = newName.trim()
        val uid = authenticatedUidForActiveUser()

        if (cleanName.isBlank() || cleanName.length < 2) {
            onFailure("Your name must contain at least 2 characters.")
            return
        }

        user = user.copy(name = cleanName)

        CoroutineScope(Dispatchers.IO).launch {
            roomDb?.userAccountDao()?.updateName(user.email, cleanName)
        }

        if (!uid.isNullOrBlank()) {
            db.collection("users")
                .document(uid)
                .update("name", cleanName)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onSuccess() }
        } else {
            onSuccess()
        }
    }

    private fun resetSession() {
        user = UserSession("", "", Role.CITIZEN)
        reports.clear()
        notifications.clear()
        syncing.clear()
        stopListening()
    }

    private fun setActiveUser(session: UserSession) {
        user = session
        sessionPrefs.edit().putString("email", session.email).apply()
    }

    // ---------------------------------------------------------
    // REPORT MANAGEMENT & FIRESTORE / ROOM OPS
    // ---------------------------------------------------------

    fun addReport(
        report: Report,
        localPhotoPath: String?,
        onFailure: ((String) -> Unit)? = null,
        onComplete: () -> Unit
    ) {
        if (user.role != Role.CITIZEN) {
            onFailure?.invoke("Only citizen accounts can submit reports.")
            return
        }
        val documentId = report.id.ifBlank {
            db.collection("reports").document().id
        }

        val ctx = appContext
        val uid = authenticatedUidForActiveUser()
        val canReachCloud = uid != null && ctx != null && isOnline(ctx)
        val reportToSave = report.copy(
            id = documentId,
            status = if (canReachCloud) report.status else ReportStatus.PENDING
        )

        // Save report to local Room database immediately
        CoroutineScope(Dispatchers.IO).launch {
            roomDb?.reportDao()?.insertOrUpdateReport(reportToSave.toEntity())
        }

        // Add to in-memory state list
        val index = reports.indexOfFirst { it.id == reportToSave.id }
        if (index != -1) {
            reports[index] = reportToSave
        } else {
            reports.add(0, reportToSave)
        }

        if (canReachCloud && uid != null) {
            saveToFirestore(reportToSave, uid, onFailure, onComplete)
        } else {
            onComplete()
        }
    }

    private fun saveToFirestore(
        report: Report,
        uid: String,
        onFailure: ((String) -> Unit)?,
        onComplete: () -> Unit
    ) {
        try {
            val reportData = hashMapOf<String, Any?>(
                "id" to report.id,
                "userId" to uid,
                "title" to report.title,
                "category" to report.category.name,
                "description" to report.description,
                "photoPath" to report.photoPath,
                "location" to report.location,
                "landmark" to report.landmark,
                "latitude" to report.latitude,
                "longitude" to report.longitude,
                "status" to report.status.name,
                "priority" to report.priority.name,
                "createdAt" to report.createdAt,
                "citizenName" to report.citizenName,
                "citizenEmail" to report.citizenEmail,
                "comments" to report.comments
            )

            db.collection("reports")
                .document(report.id)
                .set(reportData)
                .addOnSuccessListener {
                    onComplete()
                }
                .addOnFailureListener { error ->
                    val pending = report.copy(status = ReportStatus.PENDING)
                    CoroutineScope(Dispatchers.IO).launch {
                        roomDb?.reportDao()?.insertOrUpdateReport(pending.toEntity())
                    }
                    val index = reports.indexOfFirst { it.id == pending.id }
                    if (index >= 0) reports[index] = pending
                    onFailure?.invoke(error.localizedMessage ?: "Could not save report to Firestore.")
                }
        } catch (error: Exception) {
            onFailure?.invoke(error.localizedMessage ?: "An unexpected error occurred.")
        }
    }

    fun logout() {
        stopListening()
        auth.signOut()
        sessionPrefs.edit().remove("email").apply()
        resetSession()
    }

    fun startListening() {
        stopListening()
        if (user.role == Role.ADMIN) {
            reports.clear()
            return
        }
        val uid = authenticatedUidForActiveUser() ?: return

        val query = if (user.role != Role.CITIZEN) {
            db.collection("reports")
        } else {
            db.collection("reports").whereEqualTo("userId", uid)
        }

        var firstSnapshot = true

        listener = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                return@addSnapshotListener
            }

            if (!firstSnapshot) {
                for (change in snapshot.documentChanges) {
                    val report = docToReport(change.document) ?: continue
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (user.role != Role.CITIZEN) {
                                notify("OFFICERS", "New report: ${report.title}")
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            if (user.role == Role.CITIZEN) {
                                val message = when (report.status) {
                                    ReportStatus.RECEIVED -> "Your report \"${report.title}\" has been received."
                                    ReportStatus.IN_PROGRESS -> "Work has started on your report \"${report.title}\"."
                                    ReportStatus.RESOLVED -> "Your report \"${report.title}\" has been resolved."
                                    else -> "Your report \"${report.title}\" was updated."
                                }
                                notify(report.citizenEmail, message)
                            }
                        }
                        else -> Unit
                    }
                }
            }

            firstSnapshot = false

            val cloudReports = snapshot.documents.mapNotNull { docToReport(it) }

            // Persist cloud reports into Room DB
            CoroutineScope(Dispatchers.IO).launch {
                roomDb?.reportDao()?.insertOrUpdateReports(cloudReports.map { it.toEntity() })
            }

            val pendingReports = reports.filter { it.status == ReportStatus.PENDING }

            reports.clear()
            reports.addAll(cloudReports)

            pendingReports.forEach { pending ->
                if (reports.none { it.id == pending.id }) {
                    reports.add(pending)
                }
            }

            val sortedReports = reports.sortedByDescending { it.createdAt }
            reports.clear()
            reports.addAll(sortedReports)
        }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }

    private fun docToReport(document: DocumentSnapshot): Report? {
        return try {
            Report(
                id = document.getString("id") ?: document.id,
                title = document.getString("title") ?: "",
                category = runCatching {
                    ReportCategory.valueOf(document.getString("category") ?: "")
                }.getOrDefault(ReportCategory.OTHER),
                description = document.getString("description") ?: "",
                photoPath = document.getString("photoPath")?.takeIf { it.isNotBlank() },
                location = document.getString("location") ?: "",
                landmark = document.getString("landmark") ?: "",
                latitude = document.getDouble("latitude"),
                longitude = document.getDouble("longitude"),
                status = runCatching {
                    ReportStatus.valueOf(document.getString("status") ?: "")
                }.getOrDefault(ReportStatus.SUBMITTED),
                priority = runCatching {
                    Priority.valueOf(document.getString("priority") ?: "")
                }.getOrDefault(Priority.MEDIUM),
                createdAt = document.getLong("createdAt") ?: 0L,
                citizenName = document.getString("citizenName") ?: "",
                citizenEmail = document.getString("citizenEmail") ?: "",
                comments = document.getString("comments") ?: "No comment yet"
            )
        } catch (_: Exception) {
            null
        }
    }

    fun find(id: String): Report? {
        if (user.role == Role.ADMIN) return null
        return reports.firstOrNull { it.id == id }
    }

    fun myReports(): List<Report> {
        if (user.role == Role.ADMIN) return emptyList()
        return if (user.role != Role.CITIZEN) {
            reports.sortedByDescending { it.createdAt }
        } else {
            reports.filter {
                it.citizenEmail.equals(user.email, ignoreCase = true)
            }.sortedByDescending { it.createdAt }
        }
    }

    fun nextId(): String {
        var id: String
        do {
            id = "RTM-%05d".format(Random.nextInt(100000))
        } while (reports.any { it.id == id })
        return id
    }

    fun addPending(report: Report) {
        if (user.role != Role.CITIZEN) return
        val pendingReport = report.copy(status = ReportStatus.PENDING)

        // Save to Room DB for offline persistence across app restarts
        CoroutineScope(Dispatchers.IO).launch {
            roomDb?.reportDao()?.insertOrUpdateReport(pendingReport.toEntity())
        }

        if (reports.none { it.id == pendingReport.id }) {
            reports.add(0, pendingReport)
        } else {
            val index = reports.indexOfFirst { it.id == pendingReport.id }
            if (index != -1) reports[index] = pendingReport
        }

        notify(
            pendingReport.citizenEmail,
            "Report ${pendingReport.id} saved offline. It will sync when you are online."
        )
    }

    fun updateReport(
        id: String,
        status: ReportStatus,
        priority: Priority,
        comment: String,
        onFailure: ((String) -> Unit)? = null
    ) {
        val oldReport = find(id)

        if (oldReport == null) {
            onFailure?.invoke("Report not found.")
            return
        }

        if (!AdminAccountPolicy.canHandleReports(user.role)) {
            onFailure?.invoke("Only authorised officers can update reports.")
            return
        }

        val newComment = comment.ifBlank { oldReport.comments }
        val updatedReport = oldReport.copy(
            status = status,
            priority = priority,
            comments = newComment
        )

        val index = reports.indexOfFirst { it.id == id }
        if (index != -1) {
            reports[index] = updatedReport
        }

        // Persist update in Room DB
        CoroutineScope(Dispatchers.IO).launch {
            roomDb?.reportDao()?.insertOrUpdateReport(updatedReport.toEntity())
        }

        val ctx = appContext
        if (ctx != null && isOnline(ctx)) {
            db.collection("reports")
                .document(id)
                .update(
                    mapOf(
                        "status" to status.name,
                        "priority" to priority.name,
                        "comments" to newComment
                    )
                )
                .addOnFailureListener { error ->
                    onFailure?.invoke(error.localizedMessage ?: "Report update failed.")
                }
        }
    }

    fun deleteReport(
        id: String,
        onFailure: ((String) -> Unit)? = null,
        onComplete: () -> Unit
    ) {
        if (!AdminAccountPolicy.canHandleReports(user.role)) {
            onFailure?.invoke("Only authorised officers can delete reports.")
            return
        }

        reports.removeAll { it.id == id }

        CoroutineScope(Dispatchers.IO).launch {
            roomDb?.reportDao()?.deleteReportById(id)
        }

        val ctx = appContext
        if (ctx != null && isOnline(ctx)) {
            db.collection("reports")
                .document(id)
                .delete()
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener { error ->
                    onFailure?.invoke(error.localizedMessage ?: "Could not delete report.")
                }
        } else {
            onComplete()
        }
    }

    fun removeResolvedReports(onComplete: (Int) -> Unit) {
        if (!AdminAccountPolicy.canHandleReports(user.role)) {
            onComplete(0)
            return
        }

        val resolvedReports = reports.filter { it.status == ReportStatus.RESOLVED }
        if (resolvedReports.isEmpty()) {
            onComplete(0)
            return
        }

        reports.removeAll { it.status == ReportStatus.RESOLVED }

        CoroutineScope(Dispatchers.IO).launch {
            roomDb?.reportDao()?.deleteResolvedReports()
        }

        val ctx = appContext
        if (ctx != null && isOnline(ctx)) {
            val chunks = resolvedReports.chunked(500)
            var deletedCount = 0

            fun deleteNextChunk(index: Int) {
                if (index >= chunks.size) {
                    onComplete(deletedCount)
                    return
                }

                val batch = db.batch()
                chunks[index].forEach { report ->
                    val reference = db.collection("reports").document(report.id)
                    batch.delete(reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        deletedCount += chunks[index].size
                        deleteNextChunk(index + 1)
                    }
                    .addOnFailureListener {
                        onComplete(deletedCount)
                    }
            }

            deleteNextChunk(0)
        } else {
            onComplete(resolvedReports.size)
        }
    }

    fun syncPending(): Int {
        if (user.email.isBlank()) return 0

        val pendingReports = reports.filter {
            it.status == ReportStatus.PENDING && it.id !in syncing
        }

        pendingReports.forEach { pendingReport ->
            syncing.add(pendingReport.id)

            val syncedReport = pendingReport.copy(status = ReportStatus.SUBMITTED)

            addReport(
                report = syncedReport,
                localPhotoPath = pendingReport.photoPath,
                onFailure = { message ->
                    syncing.remove(pendingReport.id)
                    notify(pendingReport.citizenEmail, "Could not sync report ${pendingReport.title}: $message")
                }
            ) {
                syncing.remove(pendingReport.id)
                // Update Room DB status
                CoroutineScope(Dispatchers.IO).launch {
                    roomDb?.reportDao()?.insertOrUpdateReport(syncedReport.toEntity())
                }
                notify(pendingReport.citizenEmail, "Report \"${pendingReport.title}\" synced and sent to Roads Authority.")
            }
        }

        return pendingReports.size
    }

    private fun authenticatedUidForActiveUser(): String? {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null && firebaseUser.email.equals(user.email, ignoreCase = true)) {
            return firebaseUser.uid
        }
        return user.email.takeIf { it.isNotBlank() }
    }

    fun pendingCount(): Int {
        return reports.count {
            it.status == ReportStatus.PENDING && it.citizenEmail.equals(user.email, ignoreCase = true)
        }
    }

    // ---------------------------------------------------------
    // NOTIFICATION MANAGEMENT
    // ---------------------------------------------------------

    fun notify(
        audience: String,
        message: String,
        reportId: String? = null
    ) {
        if (audience != "OFFICERS" && !notificationsOn) return

        val id = System.currentTimeMillis() + Random.nextLong(1000)
        if (id in deletedNotificationIds) return

        notifications.add(
            0,
            AppNotification(
                id = id,
                message = message,
                time = System.currentTimeMillis(),
                audience = audience,
                reportId = reportId,
                read = false
            )
        )

        appContext?.let { ctx ->
            try {
                val channelId = "reportit_alerts"
                val intent = android.content.Intent(ctx, com.reportit.malawi.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    ctx,
                    id.toInt(),
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder = androidx.core.app.NotificationCompat.Builder(ctx, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(if (audience == "OFFICERS") "Officer Alert" else "ReportIt Malawi Update")
                    .setContentText(message)
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                val notificationManager = androidx.core.app.NotificationManagerCompat.from(ctx)
                if (androidx.core.app.ActivityCompat.checkSelfPermission(
                        ctx,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
                ) {
                    notificationManager.notify(id.toInt(), builder.build())
                }
            } catch (_: Exception) {}
        }
    }

    fun markNotificationAsRead(id: Long) {
        val index = notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = notifications[index]
            notifications[index] = item.copy(read = true)
        }
    }

    fun markAllNotificationsAsRead() {
        val currentAudience = if (user.role != Role.CITIZEN) "OFFICERS" else user.email
        for (i in notifications.indices) {
            val n = notifications[i]
            if (n.audience.equals(currentAudience, ignoreCase = true) || n.audience == "ALL") {
                notifications[i] = n.copy(read = true)
            }
        }
    }

    fun clearNotification(id: Long) {
        deletedNotificationIds.add(id)
        notifications.removeAll { it.id == id }
    }

    fun clearAllNotifications() {
        val currentAudience = if (user.role != Role.CITIZEN) "OFFICERS" else user.email
        val toClear = notifications.filter {
            it.audience.equals(currentAudience, ignoreCase = true) || it.audience == "ALL"
        }
        toClear.forEach { deletedNotificationIds.add(it.id) }
        notifications.removeAll(toClear)
    }

    fun seedDefaultNotificationsIfNeeded() {
        if (!notificationsOn) return
        val currentAudience = if (user.role != Role.CITIZEN) "OFFICERS" else user.email
        val hasExisting = notifications.any {
            (it.audience.equals(currentAudience, ignoreCase = true) || it.audience == "ALL") && it.id !in deletedNotificationIds
        }
        if (!hasExisting) {
            if (user.role != Role.CITIZEN) {
                notify("OFFICERS", "Welcome to ReportIt Officer Portal. Live infrastructure alerts activated.", null)
                reports.take(3).forEach { rep ->
                    notify("OFFICERS", "Inspection Pending: '${rep.title}' in ${rep.location}.", rep.id)
                }
            } else {
                notify(
                    user.email,
                    "Welcome to ReportIt Malawi! Report infrastructure issues and track live repair progress.",
                    null
                )
                val userReps = reports.filter { it.citizenEmail.equals(user.email, ignoreCase = true) }
                userReps.forEach { rep ->
                    notify(
                        user.email,
                        "Status Update: '${rep.title}' is currently ${statusLabel(rep.status)}.",
                        rep.id
                    )
                }
            }
        }
    }

    fun myNotifications(): List<AppNotification> {
        val audience = if (user.role != Role.CITIZEN) "OFFICERS" else user.email
        return notifications.filter {
            it.audience.equals(audience, ignoreCase = true) || it.audience == "ALL"
        }
    }

    fun loadAdminAccounts() {
        if (!AdminAccountPolicy.isAdministratorManager(user.role, user.email)) {
            adminAccounts.clear()
            return
        }
        scope.launch {
            val cachedAccounts = withContext(Dispatchers.IO) {
                roomDb?.userAccountDao()?.getAllAccounts()?.filter {
                    it.role.equals(Role.OFFICER.name, true)
                } ?: emptyList()
            }
            adminAccounts.clear()
            adminAccounts.addAll(cachedAccounts)

            val ctx = appContext
            if (ctx == null || !isOnline(ctx)) return@launch

            try {
                val response = FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("listAdminAccounts")
                    .call(null)
                    .await()
                    .data as? Map<*, *> ?: return@launch
                val accounts = (response["admins"] as? List<*>)
                    ?.mapNotNull { item ->
                        val row = item as? Map<*, *> ?: return@mapNotNull null
                        val email = row["email"]?.toString()?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        UserAccountEntity(
                            email = email,
                            name = row["name"]?.toString().orEmpty(),
                            role = Role.OFFICER.name,
                            passwordHash = "",
                            isActive = row["isActive"] as? Boolean ?: true
                        )
                    }
                    .orEmpty()

                withContext(Dispatchers.IO) {
                    accounts.forEach { roomDb?.userAccountDao()?.insertOrUpdateAccount(it) }
                }
                adminAccounts.clear()
                adminAccounts.addAll(accounts)
            } catch (_: Exception) {
                // Keep showing the last cached list if the admin list cannot be refreshed.
            }
        }
    }

    fun setAdminActive(email: String, isActive: Boolean, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        AdminAccountPolicy.validateManagement(
            user.role, user.email, email, AdminAccountPolicy.Action.CHANGE_ACCESS
        )?.let { error ->
            onFailure(error)
            return
        }
        val ctx = appContext
        if (ctx == null || !isOnline(ctx)) {
            onFailure("Connect to the internet to change administrator access.")
            return
        }
        scope.launch {
            try {
                FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("setAdminAccountActive")
                    .call(mapOf("email" to email, "isActive" to isActive))
                    .await()
                withContext(Dispatchers.IO) {
                    roomDb?.userAccountDao()?.updateActiveStatus(email, isActive)
                }
                loadAdminAccounts()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Could not update admin status.")
            }
        }
    }

    fun removeAdminAccount(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        AdminAccountPolicy.validateManagement(
            user.role, user.email, email, AdminAccountPolicy.Action.REMOVE
        )?.let { error ->
            onFailure(error)
            return
        }
        val ctx = appContext
        if (ctx == null || !isOnline(ctx)) {
            onFailure("Connect to the internet to remove an administrator.")
            return
        }
        scope.launch {
            try {
                FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("removeAdminAccount")
                    .call(mapOf("email" to email))
                    .await()
                withContext(Dispatchers.IO) {
                    roomDb?.userAccountDao()?.deleteAccount(email)
                }
                loadAdminAccounts()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Could not remove administrator.")
            }
        }
    }
}
