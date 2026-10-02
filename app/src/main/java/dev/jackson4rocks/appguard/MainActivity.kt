package dev.jackson4rocks.appguard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme

data class LaunchableApp(
    val packageName: String,
    val label: String
)

class MainActivity : ComponentActivity() {
    private lateinit var pinStore: PinStore
    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinStore = PinStore(this)

        setContent {
            AppGuardTheme {
                AppGuardScreen(
                    refreshTick = refreshTick,
                    pinStore = pinStore,
                    isServiceEnabled = isAccessibilityServiceEnabled()
                ) {
                    refreshTick++
                }
            }
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
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()

    val colors = when {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && dark ->
            dynamicDarkColorScheme(context)
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFF8FF5C7),
            secondary = Color(0xFFB1CCBE),
            background = Color(0xFF07130F),
            surface = Color(0xFF0B1814)
        )
        else -> lightColorScheme(
            primary = Color(0xFF006C4C),
            secondary = Color(0xFF4E6358),
            background = Color(0xFFF6FBF7),
            surface = Color(0xFFF6FBF7)
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

    val apps = remember(refreshTick) { loadLaunchableApps(context) }
    val locked = remember(refreshTick) { mutableStateOf(pinStore.lockedPackages()) }
    val biometricAvailable = BiometricSupport.canAuthenticate(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AppGuard", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "App lock & privacy",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Protect your apps", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (pinStore.hasPin()) {
                                "Your AppGuard PIN is configured."
                            } else {
                                "Create a PIN, choose apps, then enable the lock service."
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Security", style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 12) pin = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )

                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { if (it.length <= 12) confirm = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (pinStore.hasPin()) "Change PIN" else "Set PIN")
                        }

                        if (message != null) {
                            Text(
                                message!!,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Fingerprint / face unlock",
                                    style = MaterialTheme.typography.bodyLarge
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

                        if (biometricAvailable && pinStore.hasPin()) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(context, LockAuthActivity::class.java))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Test biometric unlock")
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Lock service", style = MaterialTheme.typography.titleMedium)
                        FilterChip(
                            selected = isServiceEnabled,
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            label = {
                                Text(
                                    if (isServiceEnabled) {
                                        "Service enabled"
                                    } else {
                                        "Enable in Accessibility settings"
                                    }
                                )
                            }
                        )
                        Text(
                            "AppGuard needs its accessibility service enabled to notice when a protected app opens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

            item {
                Text(
                    "Choose apps to lock",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(
                apps.filter { !showLockedOnly || locked.value.contains(it.packageName) },
                key = { it.packageName }
            ) { app ->
                val isLocked = locked.value.contains(app.packageName)

                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isLocked,
                            onCheckedChange = {
                                pinStore.setLocked(app.packageName, it)
                                locked.value = pinStore.lockedPackages()
                            }
                        )

                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                        ) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            if (isLocked) "Locked" else "Open",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .map { LaunchableApp(it.packageName, pm.getApplicationLabel(it).toString()) }
        .sortedBy { it.label.lowercase() }
}
