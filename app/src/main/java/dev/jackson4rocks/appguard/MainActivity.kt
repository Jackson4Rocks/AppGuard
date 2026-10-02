package dev.jackson4rocks.appguard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class LaunchableApp(
    val packageName: String,
    val label: String
)

class MainActivity : ComponentActivity() {
    private lateinit var pinStore: PinStore
    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            pinStore = PinStore(this)

            setContent {
                AppGuardTheme {
                    runCatching {
                        AppGuardScreen(
                            refreshTick = refreshTick,
                            pinStore = pinStore,
                            isServiceEnabled = runCatching {
                                isAccessibilityServiceEnabled()
                            }.getOrDefault(false),
                            onRefresh = { refreshTick++ }
                        )
                    }.getOrElse { error ->
                        Log.e(TAG, "AppGuard UI failed to compose", error)
                        StartupErrorScreen()
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "AppGuard failed during startup", t)

            setContentView(
                TextView(this).apply {
                    text = "AppGuard couldn't start.\n\nPlease restart the app."
                    textSize = 18f
                    setPadding(48, 48, 48, 48)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, AppLockAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}

@Composable
private fun AppGuardTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()

    val colors = if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF8FF5C7),
            onPrimary = Color(0xFF003827),
            primaryContainer = Color(0xFF07533B),
            onPrimaryContainer = Color(0xFFA9F8D3),
            secondary = Color(0xFFB1CCBE),
            background = Color(0xFF030A07),
            surface = Color(0xFF07130F),
            surfaceContainer = Color(0xFF0B1814),
            surfaceContainerHigh = Color(0xFF102019)
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF006C4C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF8FF5C7),
            onPrimaryContainer = Color(0xFF002116),
            secondary = Color(0xFF4E6358),
            background = Color(0xFFF3FAF5),
            surface = Color(0xFFF3FAF5),
            surfaceContainer = Color(0xFFEAF3ED),
            surfaceContainerHigh = Color(0xFFE0EAE4)
        )
    }

    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppGuardScreen(
    refreshTick: Int,
    pinStore: PinStore,
    isServiceEnabled: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var biometricEnabled by rememberSaveable { mutableStateOf(pinStore.isBiometricEnabled()) }
    var showLockedOnly by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        AppGuardSettings(
            onBack = { showSettings = false }
        )
        return
    }

    val apps = remember(refreshTick) {
        runCatching { loadLaunchableApps(context) }.getOrDefault(emptyList())
    }
    val lockedPackages = remember(refreshTick) {
        runCatching { pinStore.lockedPackages() }.getOrDefault(emptySet())
    }
    val biometricAvailable = remember {
        runCatching { BiometricSupport.canAuthenticate(context) }.getOrDefault(false)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MediumTopAppBar(
                title = {
                    Column {
                        Text(
                            "AppGuard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "App lock & privacy",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = { showSettings = true }
                    ) {
                        Text("Settings")
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isServiceEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            if (isServiceEnabled) "PROTECTION ON" else "SERVICE OFF",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isServiceEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 28.dp
            )
        ) {
            if (!isServiceEnabled) {
                item {
                    PermissionBanner {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.appguard_icon_vector),
                            contentDescription = "AppGuard",
                            modifier = Modifier.size(68.dp),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "Your apps, protected.",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when {
                                    lockedPackages.isNotEmpty() && isServiceEnabled ->
                                        "${lockedPackages.size} protected apps are ready."
                                    lockedPackages.isNotEmpty() ->
                                        "${lockedPackages.size} apps selected. Enable the service to activate protection."
                                    else ->
                                        "Choose the apps you want AppGuard to protect."
                                },
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        SummaryCard(
                            title = "Protected",
                            value = lockedPackages.size.toString(),
                            detail = "apps"
                        )
                    }
                    item {
                        SummaryCard(
                            title = "PIN",
                            value = if (pinStore.hasPin()) "ON" else "OFF",
                            detail = "required"
                        )
                    }
                    item {
                        SummaryCard(
                            title = "Biometric",
                            value = if (biometricEnabled && biometricAvailable) "ON" else "OFF",
                            detail = if (biometricAvailable) "available" else "unavailable"
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Security",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 12) pin = it.filter(Char::isDigit)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(18.dp)
                        )

                        OutlinedTextField(
                            value = confirm,
                            onValueChange = {
                                if (it.length <= 12) confirm = it.filter(Char::isDigit)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(18.dp)
                        )

                        Button(
                            onClick = {
                                when {
                                    pin.length < 4 -> message = "Use at least 4 digits."
                                    pin != confirm -> message = "PINs do not match."
                                    else -> {
                                        pinStore.setPin(pin)
                                        pin = ""
                                        confirm = ""
                                        message = "PIN saved."
                                        onRefresh()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (pinStore.hasPin()) "Change PIN" else "Set PIN")
                        }

                        message?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Fingerprint / face unlock",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (biometricAvailable) {
                                        "Use your device biometric or screen lock when unlocking."
                                    } else {
                                        "No supported biometric or device credential is available."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = biometricEnabled && biometricAvailable,
                                enabled = biometricAvailable && pinStore.hasPin(),
                                onCheckedChange = {
                                    biometricEnabled = it
                                    pinStore.setBiometricEnabled(it)
                                    onRefresh()
                                }
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        "Protected apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showLockedOnly,
                            onClick = { showLockedOnly = false },
                            label = { Text("All apps") }
                        )
                        FilterChip(
                            selected = showLockedOnly,
                            onClick = { showLockedOnly = true },
                            label = { Text("Locked") }
                        )
                    }
                }
            }

            if (apps.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Text(
                            "No launchable apps were found.",
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(
                apps.filter { !showLockedOnly || lockedPackages.contains(it.packageName) },
                key = { it.packageName }
            ) { app ->
                val checked = lockedPackages.contains(app.packageName)

                Card(
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Switch(
                            checked = checked,
                            onCheckedChange = {
                                pinStore.setLocked(app.packageName, it)
                                onRefresh()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    detail: String
) {
    Card(
        modifier = Modifier.width(132.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionBanner(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Accessibility service needs attention",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Android may show an additional security confirmation for sideloaded apps. AppGuard does not bypass that protection; enable the service from the system Accessibility settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Open Accessibility settings",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AppGuardSettings(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.appguard_icon_vector),
                            contentDescription = "AppGuard",
                            modifier = Modifier.size(76.dp)
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "Project Maintainer",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Leon Sony",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Android & Linux developer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Jackson4Rocks")
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open GitHub profile")
                }
            }

            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "AppGuard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Version 0.3.0",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "App locking is handled locally through Android's Accessibility Service and authentication APIs. No network permission is required.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupErrorScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "AppGuard couldn't load",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "The app is installed, but this device reported an error while loading the interface.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return runCatching {
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { resolveInfo ->
                resolveInfo.activityInfo?.applicationInfo
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .mapNotNull { appInfo ->
                runCatching {
                    LaunchableApp(
                        appInfo.packageName,
                        pm.getApplicationLabel(appInfo).toString()
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}

private const val TAG = "AppGuard"
