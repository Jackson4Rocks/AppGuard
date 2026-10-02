package dev.jackson4rocks.appguard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
        pinStore = PinStore(this)

        setContent {
            AppGuardTheme {
                AppGuardScreen(
                    refreshTick = refreshTick,
                    pinStore = pinStore,
                    isServiceEnabled = isAccessibilityServiceEnabled(),
                    onRefresh = { refreshTick++ }
                )
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
    val lockedPackages = remember(refreshTick) { pinStore.lockedPackages() }
    val biometricAvailable = BiometricSupport.canAuthenticate(context)

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
                            painter = painterResource(id = R.drawable.appguard_icon),
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Next
                            ),
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
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
