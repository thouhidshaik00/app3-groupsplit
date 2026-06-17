package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val viewModel: GroupSplitViewModel by viewModels {
        GroupSplitViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode, isDark = isDark) {
                GroupSplitApp(viewModel)
            }
        }
    }
}

enum class AppTab {
    TRACKER,
    BLUEPRINTS
}

enum class ActiveGroupView {
    EXPENSES,
    SETTLEMENTS,
    MEMBERS,
    AI_SOLVER,
    AI_INSIGHTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSplitApp(viewModel: GroupSplitViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
    } else {
        // Main Tracker Screen
        var showCreateGroupDialog by remember { mutableStateOf(false) }
        var showProfileDialog by remember { mutableStateOf(false) }

        val groups by viewModel.groups.collectAsStateWithLifecycle()
        val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
        val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "GroupSplit",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.seedDemoData() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("seed_demo_btn")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Demo Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Custom round Avatar for dynamic Profile & settings selection
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("top_profile_avatar")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TrackerTabContent(
                    viewModel = viewModel,
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    selectedGroup = selectedGroup,
                    onAddGroupClick = { showCreateGroupDialog = true }
                )

                // Create Group Dialog
                if (showCreateGroupDialog) {
                    CreateGroupDialog(
                        onDismiss = { showCreateGroupDialog = false },
                        onConfirm = { name, desc ->
                            viewModel.createGroup(name, desc)
                            showCreateGroupDialog = false
                        }
                    )
                }

                // Profile Dialog with Theme Selectors & Name Editing!
                if (showProfileDialog && currentUser != null) {
                    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
                    ProfileSettingsDialog(
                        currentUser = currentUser!!,
                        themeMode = themeMode,
                        isDark = isDark,
                        onDismiss = { showProfileDialog = false },
                        onUpdateName = { newName ->
                            viewModel.updateProfileName(newName)
                        },
                        onSetTheme = { selectedTheme ->
                            viewModel.setThemeMode(selectedTheme)
                        },
                        onToggleDarkMode = {
                            viewModel.setDarkMode(!isDark)
                        },
                        onSignOut = {
                            showProfileDialog = false
                            viewModel.logout()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: GroupSplitViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val recoveryStatus by viewModel.recoveryStatus.collectAsStateWithLifecycle()
    val recoveryLoading by viewModel.recoveryLoading.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Geometrically Balanced Custom Icon (Matching launcher foreground)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GroupSplit",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1.0).sp
                )
                Text(
                    text = "Geometrically Balanced Expenses",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Standard Username / Password card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sign In / Register Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username or Email") },
                        placeholder = { Text("e.g. Alice or thouhid@groupsplit.co") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("username_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("password_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showForgotPasswordDialog = true },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text("Forgot Password?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (!recoveryStatus.isNullOrEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (recoveryStatus?.contains("Incorrect") == true || recoveryStatus?.contains("Invalid") == true) 
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) 
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = recoveryStatus ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (recoveryStatus?.contains("Incorrect") == true || recoveryStatus?.contains("Invalid") == true) 
                                    MaterialTheme.colorScheme.error 
                                    else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (username.trim().isNotEmpty()) {
                                val successResult = viewModel.loginUser(username.trim(), password.trim())
                                if (successResult) {
                                    android.widget.Toast.makeText(context, "Logged in successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Please enter a username", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_in_button")
                    ) {
                        Text("Connect Securely & Sync", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Secure Recovery code Input & Reset Dialog
            if (showForgotPasswordDialog) {
                var recoveryUsername by remember { mutableStateOf(username) }
                var inputCode by remember { mutableStateOf("") }
                var newPassword by remember { mutableStateOf("") }
                val recoveryCodeSent by viewModel.recoveryCodeSent.collectAsStateWithLifecycle()

                AlertDialog(
                    onDismissRequest = {
                        if (!recoveryLoading) {
                            viewModel.clearRecoveryStatus()
                            showForgotPasswordDialog = false
                        }
                    },
                    modifier = Modifier.testTag("forgot_password_dialog"),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Secure Password Recovery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "To retrieve the verification code, enter your Username below. The backend API handles the secure dispatch and registers the session verification protocol safely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                lineHeight = 16.sp
                            )

                            // API Connection Indicator Badge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "INTEGRATED API ENDPOINT:",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "HTTPS Secure Request • httpbin.org/post",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = recoveryUsername,
                                onValueChange = { recoveryUsername = it },
                                label = { Text("Username / Email") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                enabled = !recoveryLoading,
                                modifier = Modifier.fillMaxWidth().testTag("recovery_username_input")
                            )

                            if (recoveryLoading) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Connecting to secure API server...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (recoveryCodeSent != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "📩 [SECURE EMAIL PROTOCOL]",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Verification code sent to recovery email address:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "VERIFICATION CODE: $recoveryCodeSent",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = inputCode,
                                    onValueChange = { inputCode = it },
                                    label = { Text("6-Digit Verification Code") },
                                    placeholder = { Text("Enter 6-digit code") },
                                    singleLine = true,
                                    enabled = !recoveryLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("recovery_code_input")
                                )

                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("New Secure Password") },
                                    placeholder = { Text("Enter new password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    enabled = !recoveryLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("recovery_new_pwd_input")
                                )
                            }

                            if (!recoveryStatus.isNullOrEmpty()) {
                                Text(
                                    text = recoveryStatus ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (recoveryStatus?.contains("Incorrect") == true || recoveryStatus?.contains("Invalid") == true || recoveryStatus?.contains("blank") == true)
                                        MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (recoveryCodeSent == null) {
                                    if (recoveryUsername.trim().isNotEmpty()) {
                                        viewModel.sendRecoveryVerificationCode(recoveryUsername.trim())
                                    } else {
                                        android.widget.Toast.makeText(context, "Please enter a valid Username", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val resetOk = viewModel.resetPasswordWithRecoveryCode(
                                        recoveryUsername.trim(),
                                        inputCode.trim(),
                                        newPassword.trim()
                                    )
                                    if (resetOk) {
                                        username = recoveryUsername
                                        showForgotPasswordDialog = false
                                        android.widget.Toast.makeText(context, "Password successfully updated! Try signing in now.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !recoveryLoading && recoveryUsername.trim().isNotEmpty(),
                            modifier = Modifier.testTag("recovery_confirm_btn")
                        ) {
                            Text(if (recoveryCodeSent == null) "Request Code via API" else "Reset Now & Sync")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearRecoveryStatus()
                                showForgotPasswordDialog = false
                            },
                            enabled = !recoveryLoading
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Text(
                text = "OR PAY & SYNC ENCRYPTION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.outline
            )

            // Premium Google Sign In Button
            Button(
                onClick = { showGoogleAccountPicker = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F)
                ),
                border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_login_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Simulating Google Multi-colored logo quadrant sphere
                    Canvas(modifier = Modifier.size(18.dp)) {
                        drawArc(
                            color = Color(0xFFEA4335),
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = true
                        )
                        drawArc(
                            color = Color(0xFFFBBC05),
                            startAngle = 45f,
                            sweepAngle = 90f,
                            useCenter = true
                        )
                        drawArc(
                            color = Color(0xFF34A853),
                            startAngle = 135f,
                            sweepAngle = 90f,
                            useCenter = true
                        )
                        drawArc(
                            color = Color(0xFF4285F4),
                            startAngle = 225f,
                            sweepAngle = 90f,
                            useCenter = true
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Continue with Google",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secure local SQLite vault. Encrypted token-based JWT protocol.",
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // Animated Google Account Chooser bottom sheet
        if (showGoogleAccountPicker) {
            ModalBottomSheet(
                onDismissRequest = { showGoogleAccountPicker = false },
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Choose an account",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F)
                        )
                        Text(
                            text = "to continue to GroupSplit Ledger",
                            fontSize = 13.sp,
                            color = Color(0xFF5F6368)
                        )
                    }

                    Divider(color = Color(0xFFEEEEEE))

                    // Choice 1: Thouhid Shaik (Nriit context)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleAccountPicker = false
                                viewModel.loginWithGoogle("shaikthouhid@student.nriit.ac.in", "Thouhid Shaik 🚀")
                                android.widget.Toast.makeText(context, "Google Signed In as Thouhid Shaik 🚀", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Thouhid Shaik", fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F), fontSize = 14.sp)
                            Text("shaikthouhid@student.nriit.ac.in", color = Color(0xFF5F6368), fontSize = 12.sp)
                        }
                    }

                    // Choice 2: Demo user Alice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleAccountPicker = false
                                viewModel.loginWithGoogle("alice@groupsplit.co", "Alice Smith 💡")
                                android.widget.Toast.makeText(context, "Google Signed In as Alice Smith 💡", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFCE8E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color(0xFFC5221F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Alice Smith", fontWeight = FontWeight.Bold, color = Color(0xFF1F1F1F), fontSize = 14.sp)
                            Text("alice@groupsplit.co", color = Color(0xFF5F6368), fontSize = 12.sp)
                        }
                    }

                    var typedEmail by remember { mutableStateOf("") }
                    var typedName by remember { mutableStateOf("") }
                    var showCustomInput by remember { mutableStateOf(false) }

                    if (!showCustomInput) {
                        TextButton(
                            onClick = { showCustomInput = true },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use another account", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = typedName,
                                onValueChange = { typedName = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = typedEmail,
                                onValueChange = { typedEmail = it },
                                label = { Text("Google Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (typedName.trim().isNotEmpty() && typedEmail.trim().isNotEmpty()) {
                                        showGoogleAccountPicker = false
                                        viewModel.loginWithGoogle(typedEmail.trim(), typedName.trim())
                                        android.widget.Toast.makeText(context, "Google Signed In as ${typedName.trim()}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Add & Sign In")
                            }
                        }
                    }

                    Text(
                        text = "To continue, Google will share your name, email address, profile picture, and personal preferences with GroupSplit Ledger.",
                        fontSize = 11.sp,
                        color = Color(0xFF70757A),
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsDialog(
    currentUser: User,
    themeMode: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onSignOut: () -> Unit
) {
    var editedName by remember { mutableStateOf(currentUser.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profile Settings",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Display Name editor
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Your Display Name",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (editedName.trim().isNotEmpty()) {
                                onUpdateName(editedName.trim())
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End).testTag("save_profile_button")
                    ) {
                        Text("Save Profile Name", fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Light / Dark Mode Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onToggleDarkMode() }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.Star else Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dark Theme Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDark) "Active" else "Inactive",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Custom themes selectors
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize App Theme",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAmethyst = themeMode == "amethyst"
                        Card(
                            onClick = { onSetTheme("amethyst") },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAmethyst) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                2.dp,
                                if (isAmethyst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("theme_amethyst")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Amethyst", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isAmethyst) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        val isGeometric = themeMode == "geometric"
                        Card(
                            onClick = { onSetTheme("geometric") },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isGeometric) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                2.dp,
                                if (isGeometric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("theme_geometric")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Geometric", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isGeometric) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isEmerald = themeMode == "emerald"
                        Card(
                            onClick = { onSetTheme("emerald") },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEmerald) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                2.dp,
                                if (isEmerald) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("theme_emerald")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Emerald", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isEmerald) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        val isAmber = themeMode == "amber"
                        Card(
                            onClick = { onSetTheme("amber") },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAmber) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                2.dp,
                                if (isAmber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("theme_amber")
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Sandy Amber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isAmber) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Local SQLite Vault",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "JWT SECURE ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF006A6A),
                        modifier = Modifier
                            .background(Color(0xFFCCEBEB), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_profile_button")) {
                Text("Dismiss", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onSignOut, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.testTag("sign_out_button_dialog")) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun TrackerTabContent(
    viewModel: GroupSplitViewModel,
    groups: List<Group>,
    selectedGroupId: Int?,
    selectedGroup: Group?,
    onAddGroupClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal list of groups
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Group",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                TextButton(onClick = onAddGroupClick, modifier = Modifier.testTag("add_group_btn")) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Group", fontSize = 13.sp)
                }
            }

            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No active groups yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap 'New Group' or 'Load Demo Group' to begin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groups.forEach { group ->
                        val isSelected = selectedGroupId == group.id
                        Card(
                            onClick = { viewModel.selectGroup(group.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .width(150.dp)
                                .height(72.dp)
                                .testTag("group_card_${group.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (group.description.isNotEmpty()) {
                                    Text(
                                        text = group.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Selected Group Detail Dashboard
        if (selectedGroup != null) {
            ActiveGroupDashboard(
                viewModel = viewModel,
                group = selectedGroup,
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Build Your Group & Log Shared Bills",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a group from the top list or tap 'Load Demo Group' to immediately preview live debt-splitting algorithms and real-time ledger settlement equations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.ActiveGroupDashboard(
    viewModel: GroupSplitViewModel,
    group: Group,
    modifier: Modifier = Modifier
) {
    val members by viewModel.activeGroupMembers.collectAsStateWithLifecycle()
    val expenses by viewModel.activeGroupExpenses.collectAsStateWithLifecycle()
    val splits by viewModel.activeGroupSplits.collectAsStateWithLifecycle()
    val debts by viewModel.activeGroupDebts.collectAsStateWithLifecycle()

    var activeViewTab by remember { mutableStateOf(ActiveGroupView.EXPENSES) }

    // Dynamic viewer state to simulate the premium split experience for different users
    var activeViewerId by remember(group.id, members) { mutableStateOf(members.firstOrNull()?.id) }

    // Dialog state
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    // Calculate sum analytics
    val totalExpenseAmount = expenses.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Geometric Active Viewer Selection Row
        if (members.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Viewing as:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { member ->
                        val isSelected = member.id == activeViewerId
                        val initials = member.name.take(1).uppercase()
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeViewerId = member.id },
                            label = { Text(member.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(28.dp).testTag("view_as_chip_${member.id}")
                        )
                    }
                }
            }
        }

        // Geometric Balance Overview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(28.dp), // [28px] geometric rounded corners as specified
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // bg-[#F3E7F3]
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total Group Spend",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.deleteGroup(group) },
                                modifier = Modifier.size(20.dp).testTag("delete_group_btn_${group.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = String.format("$%.2f", totalExpenseAmount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-1).sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer, // bg-[#E8DEF8]
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${members.size} Members",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer, // text-[#21005D]
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeViewer = members.find { it.id == activeViewerId }
                    val viewerName = activeViewer?.name ?: "No Member"
                    val viewerBalance = remember(activeViewerId, expenses, splits) {
                        if (activeViewerId == null) 0.0
                        else {
                            var balance = 0.0
                            for (expense in expenses) {
                                if (expense.paidByUserId == activeViewerId) {
                                    balance += expense.amount
                                }
                            }
                            for (split in splits) {
                                if (split.userId == activeViewerId) {
                                    balance -= split.shareAmount
                                }
                            }
                            balance
                        }
                    }

                    Column {
                        val absBalance = abs(viewerBalance)
                        val statusLabel = when {
                            viewerBalance > 0.01 -> "You are owed"
                            viewerBalance < -0.01 -> "You owe"
                            else -> "All settled up"
                        }
                        val statusColor = when {
                            viewerBalance > 0.01 -> Color(0xFF006A6A) // #006A6A teal green
                            viewerBalance < -0.01 -> Color(0xFFBA1A1A) // #BA1A1A red
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Text(
                            text = "$statusLabel • $viewerName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha =0.9f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = if (viewerBalance >= 0) String.format("+$%.2f", absBalance) else String.format("-$%.2f", absBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }

                    Button(
                        onClick = { activeViewTab = ActiveGroupView.SETTLEMENTS },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, // bg-[#6750A4]
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50), // pill button
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("summary_settle_up_btn")
                    ) {
                        Text(
                            text = "Settle Up",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modifierBase = Modifier.height(38.dp)

            // Expenses tab
            ViewToggleButton(
                selected = activeViewTab == ActiveGroupView.EXPENSES,
                onClick = { activeViewTab = ActiveGroupView.EXPENSES },
                label = "Ledger (${expenses.size})",
                icon = Icons.Default.List,
                modifier = modifierBase.testTag("active_tab_expenses")
            )

            // Settlements tab
            ViewToggleButton(
                selected = activeViewTab == ActiveGroupView.SETTLEMENTS,
                onClick = { activeViewTab = ActiveGroupView.SETTLEMENTS },
                label = "Settlements",
                icon = Icons.Default.Refresh,
                modifier = modifierBase.testTag("active_tab_settlements")
            )

            // Members tab
            ViewToggleButton(
                selected = activeViewTab == ActiveGroupView.MEMBERS,
                onClick = { activeViewTab = ActiveGroupView.MEMBERS },
                label = "Members (${members.size})",
                icon = Icons.Default.Person,
                modifier = modifierBase.testTag("active_tab_members")
            )

            // AI tab
            ViewToggleButton(
                selected = activeViewTab == ActiveGroupView.AI_SOLVER,
                onClick = { activeViewTab = ActiveGroupView.AI_SOLVER },
                label = "Gemini AI Advisor",
                icon = Icons.Default.Star,
                modifier = modifierBase.testTag("active_tab_ai")
            )

            // AI Insights tab
            ViewToggleButton(
                selected = activeViewTab == ActiveGroupView.AI_INSIGHTS,
                onClick = { activeViewTab = ActiveGroupView.AI_INSIGHTS },
                label = "AI Insights",
                icon = Icons.Default.Info,
                modifier = modifierBase.testTag("active_tab_ai_insights")
            )
        }

        // Content panel with Animated Content Transition
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeViewTab) {
                ActiveGroupView.EXPENSES -> {
                    ExpensesView(
                        expenses = expenses,
                        members = members,
                        splits = splits,
                        activeViewerId = activeViewerId,
                        onAddExpenseClick = { showAddExpenseDialog = true },
                        onDeleteExpenseClick = { viewModel.deleteExpense(it) },
                        viewModel = viewModel
                    )
                }
                ActiveGroupView.SETTLEMENTS -> {
                    SettlementsView(
                        debts = debts,
                        members = members,
                        expenses = expenses,
                        splits = splits,
                        viewModel = viewModel
                    )
                }
                ActiveGroupView.MEMBERS -> {
                    MembersView(
                        members = members,
                        expenses = expenses,
                        splits = splits,
                        onAddMemberClick = { showAddMemberDialog = true },
                        onRemoveMemberClick = { viewModel.removeMemberFromGroup(group.id, it) }
                    )
                }
                ActiveGroupView.AI_SOLVER -> {
                    AiSolverView(viewModel = viewModel, groupName = group.name)
                }
                ActiveGroupView.AI_INSIGHTS -> {
                    AiInsightsView(viewModel = viewModel, groupName = group.name)
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            members = members,
            viewModel = viewModel,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, amount, payerId, splitIds ->
                viewModel.addExpense(title, amount, payerId, splitIds)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name ->
                viewModel.createAndAddUserToGroup(name, group.id)
                showAddMemberDialog = false
            }
        )
    }
}

@Composable
fun ViewToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun ExpensesView(
    expenses: List<Expense>,
    members: List<User>,
    splits: List<ExpenseSplit>,
    activeViewerId: Int?,
    onAddExpenseClick: () -> Unit,
    onDeleteExpenseClick: (Int) -> Unit,
    viewModel: GroupSplitViewModel
) {
    val memberMap = members.associateBy { it.id }
    val context = LocalContext.current
    var expandedExpenseId by remember { mutableStateOf<Int?>(null) }

    // Offline sync state parameters mapping
    val isSimulatedOffline by viewModel.isSimulatedOffline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncingInProgress.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()

    val pendingSyncExpenses = expenses.filter { !it.isSynced }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fast API offline-sync and simulated connection state bar
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSimulatedOffline) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("fastapi_sync_panel"),
            border = BorderStroke(
                width = 1.dp,
                color = if (isSimulatedOffline) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (isSimulatedOffline) {
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSimulatedOffline) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isSimulatedOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Python FastAPI Sync Engine",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSimulatedOffline) "Offline Local Storage Layer Mode" else "Live Cloud Sync Active • httpbin/post",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Simulated offline toggle switch row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isSimulatedOffline) "OFFLINE" else "ONLINE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSimulatedOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Switch(
                            checked = !isSimulatedOffline,
                            onCheckedChange = { isOnline ->
                                viewModel.setSimulatedOffline(!isOnline)
                                android.widget.Toast.makeText(
                                    context, 
                                    if (isOnline) "Network Restored: Syncing local offline storage queues with Fast API..." 
                                    else "Simulating connection break. New expenses will be cached locally.", 
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.error,
                                uncheckedTrackColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .scale(0.8f)
                                .testTag("simulated_offline_switch")
                        )
                    }
                }

                // If unsynced pending expenses are queued, show synchronization action banner
                if (pendingSyncExpenses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pending Offline Queue: ${pendingSyncExpenses.size} bills cached.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Pending bills will be committed to Python FastAPI schemas securely once connection is restored.",
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerQueueSync() },
                            enabled = !isSyncing && !isSimulatedOffline,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("manually_sync_expenses_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSimulatedOffline) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Dynamic API sync response message status indicator
                if (syncStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (syncStatusMessage?.contains("Offline") == true || syncStatusMessage?.contains("queued") == true) {
                                        Icons.Default.Info
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = syncStatusMessage ?: "",
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearSyncStatusMessage() },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Expense Ledger Flow",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            FilledTonalButton(
                onClick = onAddExpenseClick,
                modifier = Modifier.testTag("open_add_expense_dialog"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No expenses logged in this group.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap 'Log Bill' to add dinner, rent, travel, or drinks.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    val payer = memberMap[expense.paidByUserId]?.name ?: "Unknown"
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    val dateStr = dateFormat.format(Date(expense.timestamp))

                    // Dynamically categorize the expense with Geometric Balance colors & symbols
                    val titleLower = expense.title.lowercase()
                    val (categoryBg, categoryText, categoryIcon) = when {
                        titleLower.contains("grocery") || titleLower.contains("groceries") || titleLower.contains("dinner") || titleLower.contains("food") || titleLower.contains("pizza") || titleLower.contains("sushi") || titleLower.contains("eat") || titleLower.contains("meal") || titleLower.contains("brunch") || titleLower.contains("lunch") || titleLower.contains("snack") || titleLower.contains("snacks") || titleLower.contains("burger") || titleLower.contains("drink") || titleLower.contains("drinks") || titleLower.contains("beer") -> {
                            Triple(Color(0xFFFFDAD6), Color(0xFF410002), Icons.Default.ShoppingCart) // Food Red
                        }
                        titleLower.contains("wifi") || titleLower.contains("internet") || titleLower.contains("cabin") || titleLower.contains("rent") || titleLower.contains("hotel") || titleLower.contains("stay") || titleLower.contains("house") || titleLower.contains("home") || titleLower.contains("lodging") -> {
                            Triple(Color(0xFFF3E7F3), Color(0xFF21005D), Icons.Default.Home) // Housing lavender
                        }
                        titleLower.contains("gas") || titleLower.contains("petrol") || titleLower.contains("taxi") || titleLower.contains("drive") || titleLower.contains("ride") || titleLower.contains("flight") || titleLower.contains("travel") || titleLower.contains("car") || titleLower.contains("road") || titleLower.contains("uber") || titleLower.contains("fuel") -> {
                            Triple(Color(0xFFD1E4FF), Color(0xFF001D36), Icons.Default.LocationOn) // Travel Light blue
                        }
                        else -> {
                            Triple(Color(0xFFEADDFF), Color(0xFF21005D), Icons.Default.Star) // Others purple
                        }
                    }

                    // Personal debt split calculations
                    val viewerSplit = splits.find { it.expenseId == expense.id && it.userId == activeViewerId }
                    val isPayer = activeViewerId != null && expense.paidByUserId == activeViewerId

                    val viewerText: String
                    val viewerAmountColor: Color
                    if (viewerSplit != null) {
                        val share = viewerSplit.shareAmount
                        if (isPayer) {
                            val owed = expense.amount - share
                            if (owed > 0.01) {
                                viewerText = String.format("You paid • Owed $%.2f", owed)
                                viewerAmountColor = Color(0xFF006A6A) // teal green
                            } else {
                                viewerText = "You paid • Fully split"
                                viewerAmountColor = MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        } else {
                            viewerText = String.format("You owe $%.2f", share)
                            viewerAmountColor = Color(0xFFBA1A1A) // red
                        }
                    } else {
                        viewerText = "Not split with you"
                        viewerAmountColor = MaterialTheme.colorScheme.outline
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedExpenseId = if (expandedExpenseId == expense.id) null else expense.id
                            }
                            .testTag("expense_card_${expense.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Dynamic Category Avatar Container
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(categoryBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon,
                                            contentDescription = "Category",
                                            tint = categoryText,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = expense.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (!expense.isSynced) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = Color(0xFFFFF1CC), // soft warm orange/amber
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "Pending Sync",
                                                            tint = Color(0xFFE28900),
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = "OFFLINE QUEUE",
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFFE28900)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Paid by ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = payer,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = " • $dateStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format("$%.2f", expense.amount),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = viewerText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = viewerAmountColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onDeleteExpenseClick(expense.id) },
                                        modifier = Modifier.size(32.dp).testTag("delete_expense_${expense.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (expandedExpenseId == expense.id) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "QR Invoice & Shareable QR Code",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GeometricQRCode(
                                            data = "groupsplit://pay?payer=${expense.paidByUserId}&amount=${expense.amount}&title=${expense.title.replace(" ", "%20")}",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant)
                                                .testTag("qr_canvas_${expense.id}")
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val splitCount = if (members.isEmpty()) 1 else members.size
                                            Text(
                                                text = "Individual Share: " + String.format("$%.2f", expense.amount / splitCount.toDouble()),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Payer: $payer",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = "Scan this dynamic QR code with any payment lens to deposit directly to $payer's balance repository.",
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            android.widget.Toast.makeText(context, "Payment settlement connection shared!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share Settlement Invite Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementsView(
    debts: List<DebtSettlement>,
    members: List<User>,
    expenses: List<Expense>,
    splits: List<ExpenseSplit>,
    viewModel: GroupSplitViewModel
) {
    val smartTipsResult by viewModel.smartTipsResult.collectAsStateWithLifecycle()
    val smartTipsLoading by viewModel.smartTipsLoading.collectAsStateWithLifecycle()

    // Query smart tips automatically upon view loading or group transaction modification
    LaunchedEffect(expenses, members) {
        viewModel.fetchSmartDebtTips()
    }

    // We can show users current absolute net balances so they understand the context of settlements
    val balances = remember(members, expenses, splits) {
        val calculated = members.associate { it.id to 0.0 }.toMutableMap()
        for (expense in expenses) {
            val payerId = expense.paidByUserId
            if (calculated.containsKey(payerId)) {
                calculated[payerId] = (calculated[payerId] ?: 0.0) + expense.amount
            }
        }
        for (split in splits) {
            val debtorId = split.userId
            if (calculated.containsKey(debtorId)) {
                calculated[debtorId] = (calculated[debtorId] ?: 0.0) - split.shareAmount
            }
        }
        calculated
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Settlement Dashboard & Standings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Balances breakdown
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Individual Net Standings",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (members.isEmpty()) {
                    Text("No members in group.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { member ->
                            val balance = balances[member.id] ?: 0.0
                            val isCreditor = balance > 0.01
                            val isZero = abs(balance) < 0.01

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when {
                                            isZero -> Color(0xFFF3E7F3)
                                            isCreditor -> Color(0xFFD1E4FF)
                                            else -> Color(0xFFFFDAD6)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = member.name, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isZero -> Color(0xFF49454F)
                                            isCreditor -> Color(0xFF001D36)
                                            else -> Color(0xFF410002)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            isZero -> "$0.00"
                                            isCreditor -> String.format("+$%.2f", balance)
                                            else -> String.format("-$%.2f", abs(balance))
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when {
                                            isZero -> Color(0xFF49454F)
                                            isCreditor -> Color(0xFF006A6A)
                                            else -> Color(0xFFBA1A1A)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Smart Debt Tips Card (Gemini Integration)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("smart_debt_tips_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Debt Tips",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GEMINI AI",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.fetchSmartDebtTips() },
                            modifier = Modifier.size(24.dp).testTag("refresh_tips_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Tips",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                if (smartTipsLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyzing group spend patterns...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else if (!smartTipsResult.isNullOrEmpty()) {
                    Text(
                        text = smartTipsResult ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp,
                        modifier = Modifier.testTag("smart_tips_content_text")
                    )
                } else {
                    Text(
                        text = "No pattern telemetry resolved. Register a dynamic bill to query financial AI advice.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Settlements breakdown list
        Text(
            text = "Optimized Multi-User Transactions",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (debts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF006A6A).copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "All settled up!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "No pending transactions inside this group.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(debts) { settlement ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                // Mini avatar or indicator
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFDAD6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = settlement.debtor.name.take(1).uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF410002)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = settlement.debtor.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFBA1A1A),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "owes",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = settlement.creditor.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF006A6A),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = "Direct bank transfer suggested",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add, // simple indicator of transfer flow
                                    contentDescription = null,
                                    tint = Color(0xFF006A6A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("$%.2f", settlement.amount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MembersView(
    members: List<User>,
    expenses: List<Expense>,
    splits: List<ExpenseSplit>,
    onAddMemberClick: () -> Unit,
    onRemoveMemberClick: (Int) -> Unit
) {
    val balances = remember(members, expenses, splits) {
        val calculated = members.associate { it.id to 0.0 }.toMutableMap()
        expenses.forEach { exp ->
            val splitsForExp = splits.filter { it.expenseId == exp.id }
            if (splitsForExp.isNotEmpty()) {
                val payerId = exp.paidByUserId
                calculated[payerId] = (calculated[payerId] ?: 0.0) + exp.amount
                splitsForExp.forEach { s ->
                    val shareId = s.userId
                    calculated[shareId] = (calculated[shareId] ?: 0.0) - s.shareAmount
                }
            } else {
                // equal split fallback if no split entries
                val payerId = exp.paidByUserId
                calculated[payerId] = (calculated[payerId] ?: 0.0) + exp.amount
                val share = exp.amount / members.size.coerceAtLeast(1)
                members.forEach { m ->
                    calculated[m.id] = (calculated[m.id] ?: 0.0) - share
                }
            }
        }
        calculated
    }

    val totalOutlay = remember(expenses) { expenses.sumOf { it.amount } }
    val avgShare = remember(totalOutlay, members) { totalOutlay / members.size.coerceAtLeast(1) }
    val txnCount = remember(expenses) { expenses.size }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dashboard & Participants",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            FilledTonalButton(
                onClick = onAddMemberClick,
                modifier = Modifier.testTag("open_add_member_dialog"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No participants found in this group.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap 'Add Member' to input friends to split costs with.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Finance Dashboard Summary Item
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("group_dashboard_summary_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Real-time Debt Statuses",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE STATEMENT",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Outlay metric
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Total Group Spend",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format("$%.2f", totalOutlay),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                // Average share metric
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Avg Share/Member",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format("$%.2f", avgShare),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                // Transactions count
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Bills Recorded",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$txnCount invoice${if (txnCount == 1) "" else "s"}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                items(members, key = { it.id }) { member ->
                    val userBalance = balances[member.id] ?: 0.0
                    val totalPaidByMember = expenses.filter { it.paidByUserId == member.id }.sumOf { it.amount }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_dashboard_card_${member.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF3E7F3)), // soft lilac
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.name.take(1).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF21005D)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = member.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Paid: " + String.format("$%.2f", totalPaidByMember),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                // Balance indicator
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    val absBal = kotlin.math.abs(userBalance)
                                    val (color, text, subText) = when {
                                        userBalance > 0.01 -> Triple(
                                            Color(0xFF006A6A),
                                            String.format("+$%.2f", absBal),
                                            "is owed"
                                        )
                                        userBalance < -0.01 -> Triple(
                                            Color(0xFFBA1A1A),
                                            String.format("-$%.2f", absBal),
                                            "owes"
                                        )
                                        else -> Triple(
                                            MaterialTheme.colorScheme.outline,
                                            String.format("$%.2f", 0.0),
                                            "settled"
                                        )
                                    }

                                    Text(
                                        text = text,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = color,
                                        modifier = Modifier.testTag("member_balance_${member.id}")
                                    )
                                    Text(
                                        text = subText,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color.copy(alpha = 0.85f)
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveMemberClick(member.id) },
                                    modifier = Modifier.size(32.dp).testTag("remove_member_${member.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove From Group",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialogs
@Composable
fun CreateGroupDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (e.g. Ski Trip 🏔️)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_group_name")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_group_desc")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, desc) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_group")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_create_group")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Participant") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Enter Name (e.g. Bob)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_member_name")
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_member")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_add_member")) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddExpenseDialog(
    members: List<User>,
    viewModel: GroupSplitViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, List<Int>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var payerUserId by remember { mutableStateOf<Int?>(members.firstOrNull()?.id) }

    // Multi-member split selection state (defaults to all members)
    val selectedSplitUserIds = remember { mutableStateListOf<Int>().apply { addAll(members.map { it.id }) } }

    val receiptResult by viewModel.receiptParseResult.collectAsStateWithLifecycle()
    val receiptLoading by viewModel.receiptParseLoading.collectAsStateWithLifecycle()

    var showScanner by remember { mutableStateOf(false) }
    var receiptText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.clearReceiptParseResult()
    }

    LaunchedEffect(receiptResult) {
        receiptResult?.let { res ->
            if (res.title.isNotEmpty() && !res.title.contains("Error Parse", ignoreCase = true)) {
                title = res.title
                amountStr = if (res.amount > 0.0) res.amount.toString() else ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New Shared Bill") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // AI Receipt Parser / Bill autofiller helper
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Gemini AI Bill Scanner",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = { 
                                    showScanner = !showScanner 
                                    if (!showScanner) {
                                        receiptText = ""
                                        viewModel.clearReceiptParseResult()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (showScanner) "Hide Parser" else "Try AI Scanner",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showScanner) {
                            Text(
                                text = "Paste raw receipt text or tap a demo template below. Gemini extracts the merchant title and parsed amounts automatically.",
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = receiptText,
                                onValueChange = { receiptText = it },
                                placeholder = { Text("Paste receipt text, SMS details or dining lines here...", fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .testTag("ai_receipt_raw_input"),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = false,
                                maxLines = 4
                            )

                            Text(
                                text = "Demo Templates (Tap to simulation scan):",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val templates = listOf(
                                    "Pizza Palace" to "PIZZA PALACE RESTAURANT\n2x Cheese Pizza - 24.00\n1x Soda - 3.50\nTax 8% - 2.20\nTOTAL BILL DUE: \$29.70\nThank you!",
                                    "Uber Ride" to "UBER TECHNOLOGIES INC.\nRide Trip ID #77ac90\nDistance: 12 km\nSubtotal: 18.50 \nTip: 4.00 \nTotal Paid: \$22.50 via Visa",
                                    "Gas Depot" to "SHELL FUEL STATION\n87 Octane Gas\nGallons: 11.5g @ \$3.10/g\nTotal Amount: \$35.65\nCash Tendered: \$40.00\nChange: \$4.35"
                                )

                                templates.forEach { (name, rawText) ->
                                    AssistChip(
                                        onClick = {
                                            receiptText = rawText
                                            viewModel.parseReceiptText(rawText)
                                        },
                                        label = { Text(name, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.parseReceiptText(receiptText) },
                                enabled = receiptText.isNotBlank() && !receiptLoading,
                                modifier = Modifier.fillMaxWidth().height(36.dp).testTag("ai_parse_receipt_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (receiptLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyze & Autofill Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            receiptResult?.let { res ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Extracted: ${res.title} • \$${res.amount}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Metadata: ${res.serviceNotes ?: "Successfully mapped to inputs!"}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Airbnb, Dinner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_title")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Amount ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount")
                )

                // Select payer options
                Text("Who paid for this?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { member ->
                        val isPayer = payerUserId == member.id
                        FilterChip(
                            selected = isPayer,
                            onClick = { payerUserId = member.id },
                            label = { Text(member.name, fontSize = 11.sp) },
                            modifier = Modifier.testTag("payer_chip_${member.id}")
                        )
                    }
                }

                // Select split participants options
                Text("Split among whom?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                members.forEach { member ->
                    val isChecked = selectedSplitUserIds.contains(member.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) {
                                    if (selectedSplitUserIds.size > 1) {
                                        selectedSplitUserIds.remove(member.id)
                                    }
                                } else {
                                    selectedSplitUserIds.add(member.id)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedSplitUserIds.add(member.id)
                                } else {
                                    if (selectedSplitUserIds.size > 1) {
                                        selectedSplitUserIds.remove(member.id)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("split_checkbox_${member.id}")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(member.name, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val isValid = title.isNotBlank() && amount > 0.0 && payerUserId != null && selectedSplitUserIds.isNotEmpty()

            Button(
                onClick = {
                    payerUserId?.let { payer ->
                        onConfirm(title, amount, payer, selectedSplitUserIds.toList())
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_add_expense")
            ) {
                Text("Add Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_add_expense")) {
                Text("Cancel")
            }
        }
    )
}

enum class BlueprintCategory {
    CORE_ARCH,
    SQLALCHEMY_SCHEMA,
    PROJECT_DIR,
    DEBT_ALGO
}

@Composable
fun BlueprintsDocumentationTab() {
    var activeCategory by remember { mutableStateOf(BlueprintCategory.CORE_ARCH) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "GroupSplit Architecture Blueprint",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Explore the complete roadmap, folder map, and production SQL code written in SQLAlchemy models to support multi-user settlement calculations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BlueprintChip(BlueprintCategory.CORE_ARCH, activeCategory == BlueprintCategory.CORE_ARCH) { activeCategory = BlueprintCategory.CORE_ARCH }
            BlueprintChip(BlueprintCategory.SQLALCHEMY_SCHEMA, activeCategory == BlueprintCategory.SQLALCHEMY_SCHEMA) { activeCategory = BlueprintCategory.SQLALCHEMY_SCHEMA }
            BlueprintChip(BlueprintCategory.PROJECT_DIR, activeCategory == BlueprintCategory.PROJECT_DIR) { activeCategory = BlueprintCategory.PROJECT_DIR }
            BlueprintChip(BlueprintCategory.DEBT_ALGO, activeCategory == BlueprintCategory.DEBT_ALGO) { activeCategory = BlueprintCategory.DEBT_ALGO }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Detailed Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (activeCategory) {
                    BlueprintCategory.CORE_ARCH -> CoreArchitectureBlueprintView()
                    BlueprintCategory.SQLALCHEMY_SCHEMA -> SQLAlchemyModelBlueprintView()
                    BlueprintCategory.PROJECT_DIR -> ProjectFolderBlueprintView()
                    BlueprintCategory.DEBT_ALGO -> MathematicalLogicBlueprintView()
                }
            }
        }
    }
}

@Composable
fun BlueprintChip(category: BlueprintCategory, selected: Boolean, onClick: () -> Unit) {
    val label = when (category) {
        BlueprintCategory.CORE_ARCH -> "🛰️ System Architecture"
        BlueprintCategory.SQLALCHEMY_SCHEMA -> "🗄️ PostgreSQL Schema"
        BlueprintCategory.PROJECT_DIR -> "📁 Project Directory"
        BlueprintCategory.DEBT_ALGO -> "🧮 Mathematical Logic"
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag("blueprint_chip_${category.name}")
    )
}

@Composable
fun CoreArchitectureBlueprintView() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BlueprintHeader("🛰️ System Architecture Blueprint")

        BlueprintText(
            "Our multi-user split application operates on an asynchronous and robust client-server decoupled dynamic:\n\n" +
            "1. Client UI Core (Android/Jetpack Compose or React Native/Flutter):\n" +
            "   - Stores local state using SQLite (Room or Hive) for instant offline reads.\n" +
            "   - Syncs delta modifications with the Python backend API in the background.\n" +
            "   - Renders a reactive ledger and recalculates balances instantly.\n\n" +
            "2. RESTful Backend Engine (Python / FastAPI):\n" +
            "   - Built as stateless HTTP microservices powered by FastAPI (high async throughput).\n" +
            "   - Relies on PostgreSQL as the centralized relational source of truth."
        )

        BlueprintSubHeader("Calculation Strategy: Client vs. Server Decision")

        BlueprintAlert(
            "DECISION: The server MUST act as the absolute engine of truth for all mathematical calculations (ledger net balance aggregations & transaction settlements), but local clients can execute identical offline algorithms safely for latency suppression.",
            Color(0xFFE3F2FD),
            Color(0xFF0D47A1)
        )

        BlueprintText(
            "Why Server-Side calculation wins:\n" +
            "• Absolute Truth: Prevents divergent balances caused by clock drifts or inconsistent client float precision.\n" +
            "• Batch Runs: Can easily resolve calculations in SQL transactions across huge datasets.\n" +
            "• Easy Updates: Adjusting algorithms (e.g. adding weight weights, currency adjustments) is immediately live for all devices without requiring app store updates."
        )
    }
}

@Composable
fun SQLAlchemyModelBlueprintView() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BlueprintHeader("🗄️ SQLAlchemy Relational Database Schema")

        BlueprintText(
            "This third-normal-form (3NF) PostgreSQL architecture solves many-to-many relationships safely. " +
            "Any updates (e.g. a user altering their username) cascade correctly without database redundancies."
        )

        // Code snippet inside card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "models.py (SQLAlchemy 2.0 Syntax)",
                    color = Color(0xFF80CBC4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = """
from datetime import datetime
from typing import List, Optional
from decimal import Decimal
from sqlalchemy import String, ForeignKey, Table, Column, Integer, Numeric, DateTime
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship

class Base(DeclarativeBase):
    pass

# Many-to-Many association table for Users in Groups
group_user_association = Table(
    "group_user",
    Base.metadata,
    Column("group_id", Integer, ForeignKey("groups.id", ondelete="CASCADE"), primary_key=True),
    Column("user_id", Integer, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
)

class User(Base):
    __tablename__ = "users"
    
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(50))
    email: Mapped[str] = mapped_column(String(100), unique=True, index=True)
    
    # Relationships
    groups: Mapped[List["Group"]] = relationship(
        secondary=group_user_association, back_populates="members"
    )
    expenses_paid: Mapped[List["Expense"]] = relationship(back_populates="paid_by")
    shares_owed: Mapped[List["ExpenseSplit"]] = relationship(back_populates="user")


class Group(Base):
    __tablename__ = "groups"
    
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100))
    description: Mapped[Optional[str]] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(default=datetime.utcnow)
    
    # Relationships
    members: Mapped[List["User"]] = relationship(
        secondary=group_user_association, back_populates="groups"
    )
    expenses: Mapped[List["Expense"]] = relationship(back_populates="group", cascade="all, delete-orphan")


class Expense(Base):
    __tablename__ = "expenses"
    
    id: Mapped[int] = mapped_column(primary_key=True)
    group_id: Mapped[int] = mapped_column(ForeignKey("groups.id", ondelete="CASCADE"))
    title: Mapped[str] = mapped_column(String(100))
    amount: Mapped[Decimal] = mapped_column(Numeric(10, 2))  # Decimal protects currency values
    paid_by_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="RESTRICT"))
    timestamp: Mapped[datetime] = mapped_column(default=datetime.utcnow)
    
    # Relationships
    group: Mapped["Group"] = relationship(back_populates="expenses")
    paid_by: Mapped["User"] = relationship(back_populates="expenses_paid")
    splits: Mapped[List["ExpenseSplit"]] = relationship(back_populates="expense", cascade="all, delete-orphan")


class ExpenseSplit(Base):
    __tablename__ = "expense_splits"
    
    expense_id: Mapped[int] = mapped_column(ForeignKey("expenses.id", ondelete="CASCADE"), primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    share_amount: Mapped[Decimal] = mapped_column(Numeric(10, 2))
    
    # Relationships
    expense: Mapped["Expense"] = relationship(back_populates="splits")
    user: Mapped["User"] = relationship(back_populates="shares_owed")
""".trimIndent(),
                    color = Color(0xFFECEFF1),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ProjectFolderBlueprintView() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BlueprintHeader("📁 Python FastAPI Backend Project Map")

        BlueprintText(
            "An enterprise modular directory layout separates system configurations, DB persistence, business routes, and tests."
        )

        // Project tree drawing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = """
backend/
├── app/
│   ├── __init__.py
│   ├── config.py           # Handles env variables & database connection configs
│   ├── main.py             # FastAPI entrypoint, registers models & routers
│   ├── database.py         # SQLAlchemy engine, sessionmakers and unit-of-work
│   ├── models/             # SQLAlchemy tabular entities (3NF normalization)
│   │   ├── __init__.py
│   │   ├── associations.py # Association helpers (group_user)
│   │   ├── user.py
│   │   ├── group.py
│   │   └── expense.py
│   ├── schemas/            # Pydantic schemas validating API dynamic payloads
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── group.py
│   │   └── expense.py
│   ├── routers/            # FastAPI REST routes (handlers)
│   │   ├── __init__.py
│   │   ├── users.py
│   │   ├── groups.py
│   │   └── expenses.py
│   └── services/           # CORE BUSINESS LOGIC (Debt minimization calculations)
│       ├── __init__.py
│       └── settlement.py   # Pure Greedy Debt settlement algorithms
├── tests/                  # Integrity PyTest blocks
│   ├── __init__.py
│   ├── test_settle.py      # Settle math test models
│   └── test_api.py         # API integration unit tests
├── alembic.ini             # DB migrations configuration
├── requirements.txt        # Backend dependencies list
└── Dockerfile              # Dockerized target file
""".trimIndent(),
                color = Color(0xFFA5D6A7),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun MathematicalLogicBlueprintView() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BlueprintHeader("🧮 Settle-Debts Ledger Algorithm")

        BlueprintText(
            "To simplify payments (e.g. avoiding Alice paying Bob 10 dollars, and Bob paying Charlie 10 dollars, resolving directly into Alice paying Charlie 10 dollars), we use a flow-optimizing greedy matching settlement algorithm:\n\n" +
            "1. Compute Net Balances:\n" +
            "   - Net balance B_i for each user i is calculated as total payments minus total shares logged:\n" +
            "     B_i = SUM(Payments by i) - SUM(Owed share of i)\n\n" +
            "2. Segment the graph into Credits and Debts:\n" +
            "   - Creditors (B_i > 0): Members who are owed money.\n" +
            "   - Debtors (B_i < 0): Members who owe money.\n\n" +
            "3. Greedy Settlement Flow:\n" +
            "   - Sort both groups by magnitude (descending).\n" +
            "   - Match the largest debtor with the largest creditor.\n" +
            "   - Calculate settlement amount: S = min(|Balance of debtor|, Balance of creditor).\n" +
            "   - Form a direct ledger transfer: Debtor pays Creditor S.\n" +
            "   - Deduct S from both parties and iterate until everyone resolves to 0.00 balance."
        )

        BlueprintSubHeader("Complexity Overview")
        BlueprintText(
            "• Time Complexity: O(N log N) where N represents the number of group members. (Sorting dominates, matching executes in linear O(N) cycles).\n" +
            "• Space Complexity: O(N) to isolate debtors and creditors lists."
        )
    }
}

@Composable
fun BlueprintHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun BlueprintSubHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
fun BlueprintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp
    )
}

@Composable
fun BlueprintAlert(text: String, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, shape = RoundedCornerShape(8.dp))
            .border(1.dp, contentColor.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun AiSolverView(viewModel: GroupSplitViewModel, groupName: String) {
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    var customQuestion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Geometric Balance AI Advisor",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "Get real-time, mathematically optimized suggestions on settling up debts inside the group '$groupName'. Ask questions about specific splits, or request general debt-minimization matrices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Custom Question Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Custom AI Inquiry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    placeholder = { Text("e.g. Who owes the most? How do we split gas?", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("ai_custom_input"),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = false,
                    maxLines = 3
                )

                // Suggested AI prompt assistant chips
                Text(
                    text = "Recommended Preset Inquiries:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Who owes most?" to "Who owes the most right now, and what is the fastest settlement path for them?",
                        "Analyze trends" to "Analyze our spending category categories and point out who is dominating the payments.",
                        "Optimized splitting" to "Suggest 3 unique geometric smart budget rules matching our expense patterns to save money."
                    )
                    presets.forEach { (label, promptText) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable {
                                customQuestion = promptText
                                viewModel.askAiSolver(promptText)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.askAiSolver(if (customQuestion.trim().isNotEmpty()) customQuestion.trim() else null) },
                        enabled = !aiLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("ai_solve_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (aiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compute AI Synthesis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (aiResult != null && !aiLoading) {
                        OutlinedButton(
                            onClick = { viewModel.askAiSolver(null) },
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("General Advice", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Analysis Result View
        if (aiLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Connecting to Direct Gemini REST API...",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Calculating group balance structures & solving sparse debt matrices.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (aiResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Resolution Sheet",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GEMINI-2.5-FLASH",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = aiResult ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("ai_result_text")
                    )
                }
            }
        } else {
            // Empty State
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No AI analysis computed yet.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Compute AI Synthesis' above to invoke geometric algorithms.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GeometricQRCode(data: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val size = size.minDimension
        val cellSize = size / 21f

        // Draw clean white backdrop
        drawRect(color = Color.White)

        // Deterministic bit checker using string hashes
        val dataHash = data.hashCode()
        fun isCellActive(r: Int, c: Int): Boolean {
            // Corner finder anchor layouts (Top-Left, Top-Right, Bottom-Left)
            if (r < 7 && c < 7) return false
            if (r < 7 && c >= 14) return false
            if (r >= 14 && c < 7) return false

            // Pattern design lines & hashed content logic
            val cellHash = (r * 11113 + c * 22273) xor dataHash
            return (cellHash % 3 == 0) || (cellHash % 7 == 1)
        }

        // Inner function to design 7x7 corner eye anchors
        fun drawFinderPattern(offsetX: Float, offsetY: Float) {
            // Outer 7x7 rectangle
            drawRect(
                color = Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(offsetX, offsetY),
                size = androidx.compose.ui.geometry.Size(cellSize * 7, cellSize * 7)
            )
            // Middle 5x5 negative spacer
            drawRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(offsetX + cellSize, offsetY + cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize * 5, cellSize * 5)
            )
            // Inner 3x3 core visual eye
            drawRect(
                color = Color.Black,
                topLeft = androidx.compose.ui.geometry.Offset(offsetX + cellSize * 2, offsetY + cellSize * 2),
                size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3)
            )
        }

        // Draw top-left anchor, top-right anchor, bottom-left anchor
        drawFinderPattern(0f, 0f)
        drawFinderPattern(cellSize * 14, 0f)
        drawFinderPattern(0f, cellSize * 14)

        // Draw randomized QR-style hashed dot arrays
        for (r in 0 until 21) {
            for (c in 0 until 21) {
                if (isCellActive(r, c)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(c * cellSize, r * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

@Composable
fun AiInsightsView(viewModel: GroupSplitViewModel, groupName: String) {
    val expenses by viewModel.activeGroupExpenses.collectAsStateWithLifecycle()
    val members by viewModel.activeGroupMembers.collectAsStateWithLifecycle()
    val aiInsightsResult by viewModel.aiInsightsResult.collectAsStateWithLifecycle()
    val aiInsightsLoading by viewModel.aiInsightsLoading.collectAsStateWithLifecycle()

    LaunchedEffect(expenses) {
        if (expenses.isNotEmpty()) {
            viewModel.fetchAiCategoryInsights()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI Spending Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Live analytics and cost drivers of '$groupName'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Visual Category Dashboard
        if (expenses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Expenses Logged",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add dining bills, transport bills or ticket splits first to unlock category-wide intelligence diagnostics.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            val totalSpend = expenses.sumOf { it.amount }
            
            // Helper category classifier inside
            val categoryConfig = mapOf(
                "Dining" to Pair(Color(0xFFE07A5F), Icons.Default.Notifications),
                "Transport" to Pair(Color(0xFF3D5A80), Icons.Default.Place),
                "Lodging" to Pair(Color(0xFF81B29A), Icons.Default.Home),
                "Entertainment" to Pair(Color(0xFFF2CC8F), Icons.Default.Star),
                "Groceries" to Pair(Color(0xFF8338EC), Icons.Default.ShoppingCart),
                "Utilities" to Pair(Color(0xFF3A86C8), Icons.Default.Build),
                "General" to Pair(Color(0xFF6C757D), Icons.Default.List)
            )

            fun getCategoryFromTitleLocal(title: String): String {
                val t = title.lowercase()
                return when {
                    t.contains("food") || t.contains("dining") || t.contains("dine") || t.contains("eat") || t.contains("lunch") || t.contains("dinner") || t.contains("restaurant") || t.contains("cafe") || t.contains("meal") || t.contains("starbucks") || t.contains("mac") || t.contains("mcdonald") || t.contains("pizza") || t.contains("burger") || t.contains("bakery") || t.contains("baskin") || t.contains("donut") || t.contains("cream") -> "Dining"
                    t.contains("taxi") || t.contains("uber") || t.contains("cab") || t.contains("gas") || t.contains("fuel") || t.contains("flight") || t.contains("transit") || t.contains("car") || t.contains("ride") || t.contains("travel") || t.contains("train") || t.contains("toll") || t.contains("bus") || t.contains("shell") || t.contains("chevron") || t.contains("mobil") || t.contains("bp") -> "Transport"
                    t.contains("hotel") || t.contains("stay") || t.contains("airbnb") || t.contains("lodging") || t.contains("rent") || t.contains("room") || t.contains("hostel") || t.contains("booking") -> "Lodging"
                    t.contains("movie") || t.contains("show") || t.contains("ticket") || t.contains("concert") || t.contains("game") || t.contains("event") || t.contains("fun") || t.contains("theme") || t.contains("park") || t.contains("museum") || t.contains("play") || t.contains("netflix") || t.contains("spotify") || t.contains("hulu") || t.contains("disney") -> "Entertainment"
                    t.contains("grocery") || t.contains("groceries") || t.contains("market") || t.contains("supermarket") || t.contains("walmart") || t.contains("target") || t.contains("aldi") || t.contains("costco") || t.contains("kroger") || t.contains("whole foods") || t.contains("food lion") || t.contains("trader joe") -> "Groceries"
                    t.contains("bill") || t.contains("utility") || t.contains("power") || t.contains("water") || t.contains("wifi") || t.contains("internet") || t.contains("phone") || t.contains("electricity") || t.contains("sewer") || t.contains("trash") -> "Utilities"
                    else -> "General"
                }
            }

            val categoryMap = mutableMapOf<String, Double>()
            expenses.forEach { exp ->
                val cat = getCategoryFromTitleLocal(exp.title)
                categoryMap[cat] = (categoryMap[cat] ?: 0.0) + exp.amount
            }

            val sortedCategories = categoryMap.toList().sortedByDescending { it.second }

            // Category list summary report Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("ai_category_spending_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Spending Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Total: $${String.format("%.2f", totalSpend)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    sortedCategories.forEach { (cat, amt) ->
                        val (color, icon) = categoryConfig[cat] ?: Pair(Color(0xFF6C757D), Icons.Default.List)
                        val percentage = (amt / totalSpend).toFloat()

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = cat,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$${String.format("%.2f", amt)} (${String.format("%.1f", percentage * 100)}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            
                            // Visual horizontal fill indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(50)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(percentage)
                                        .background(color = color, shape = RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }
            }

            // Gemini Deep Diagnostics Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Gemini AI Diagnostics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.fetchAiCategoryInsights() },
                            enabled = !aiInsightsLoading,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp).testTag("ai_insights_refresh_btn"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Refresh", fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    if (aiInsightsLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Analyzing categories and finding cost clusters...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else if (aiInsightsResult != null) {
                        // High-fidelity structured markdown parsing of categories
                        val lines = aiInsightsResult!!.lines()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_insights_result_container")
                        ) {
                            lines.forEach { line ->
                                val trimmed = line.trim()
                                if (trimmed.startsWith("📊") || trimmed.startsWith("💡") || trimmed.startsWith("🏷️") || trimmed.startsWith("📈")) {
                                    Text(
                                        text = trimmed,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                } else if (trimmed.startsWith("*   **") || trimmed.startsWith("* **") || trimmed.startsWith("- **")) {
                                    // Highlight list with visual accent card
                                    val contentText = trimmed.replace(Regex("^[*\\-\\d.]+\\s*"), "")
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                            )
                                            Text(
                                                text = contentText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                } else if (trimmed.startsWith("1.") || trimmed.startsWith("2.") || trimmed.startsWith("3.")) {
                                    val contentText = trimmed.replace(Regex("^\\d+\\.\\s*"), "")
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "★",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = contentText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                } else if (trimmed.isNotEmpty()) {
                                    Text(
                                        text = trimmed,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Unanalyzed empty state inside Diagnostics card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Category Insights Ready",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Button(
                                onClick = { viewModel.fetchAiCategoryInsights() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("ai_insights_trigger_btn")
                            ) {
                                Text("Inquire AI Category Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
