package dev.jackson4rocks.appguard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private enum class GuardTab(val label: String) {
    HOME("Home"),
    APPS("Apps"),
    SECURITY("Security"),
    MORE("More")
}

class AppGuardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppGuardTheme {
                AppGuardApp()
            }
        }
    }
}

@Composable
private fun AppGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF8FF5C7),
            onPrimary = Color(0xFF003827),
            primaryContainer = Color(0xFF07533B),
            onPrimaryContainer = Color(0xFFA9F8D3),
            secondary = Color(0xFFB1CCBE),
            background = Color(0xFF030A07),
            surface = Color(0xFF07130F),
            surfaceContainer = Color(0xFF0B1814),
            surfaceContainerHigh = Color(0xFF102019)
        ),
        content = content
    )
}

@Composable
private fun AppGuardApp() {
    val context = LocalContext.current
    val pinStore = remember { PinStore(context) }
    val lockPreferences = remember { LockPreferences(context) }
    var currentTab by rememberSaveable { mutableStateOf(GuardTab.HOME.name) }
    var refresh by remember { mutableIntStateOf(0) }

    val apps = remember(refresh) { loadLaunchableApps(context) }
    val locked = remember(refresh) { pinStore.lockedPackages() }
    val serviceEnabled = remember(refresh) { isAccessibilityServiceEnabled(context) }

    BackHandler(enabled = currentTab != GuardTab.HOME.name) {
        currentTab = GuardTab.HOME.name
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                GuardTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab.name,
                        onClick = { currentTab = tab.name },
                        icon = {
    when (tab) {
        GuardTab.HOME -> androidx.compose.material3.Icon(Icons.Default.Home, contentDescription = null)
        GuardTab.APPS -> androidx.compose.material3.Icon(Icons.Default.Apps, contentDescription = null)
        GuardTab.SECURITY -> androidx.compose.material3.Icon(Icons.Default.Security, contentDescription = null)
        GuardTab.MORE -> androidx.compose.material3.Icon(Icons.Default.MoreHoriz, contentDescription = null)
    }
},
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (GuardTab.valueOf(currentTab)) {
            GuardTab.HOME -> HomeTab(
                padding = padding,
                lockedCount = locked.size,
                pinSet = pinStore.hasPin(),
                serviceEnabled = serviceEnabled,
                openApps = { currentTab = GuardTab.APPS.name },
                openSecurity = { currentTab = GuardTab.SECURITY.name }
            )
            GuardTab.APPS -> AppsTab(
                padding = padding,
                apps = apps,
                locked = locked,
                pinStore = pinStore
            ) { refresh++ }
            GuardTab.SECURITY -> SecurityTab(
                padding = padding,
                pinStore = pinStore,
                serviceEnabled = serviceEnabled
            ) { refresh++ }
            GuardTab.MORE -> MoreTab(
                padding = padding,
                lockPreferences = lockPreferences,
                context = context
            )
        }
    }
}

@Composable
private fun HomeTab(
    padding: PaddingValues,
    lockedCount: Int,
    pinSet: Boolean,
    serviceEnabled: Boolean,
    openApps: () -> Unit,
    openSecurity: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Text("AppGuard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Your privacy, at a glance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.appguard_icon_vector),
                            contentDescription = "AppGuard",
                            modifier = Modifier.size(74.dp).clip(RoundedCornerShape(22.dp))
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (lockedCount == 0) "Protected apps" else "${lockedCount} apps protected",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                when {
                                    !pinSet -> "Set your PIN in Security to start protecting apps."
                                    !serviceEnabled -> "Finish the Android Accessibility setup to activate locking."
                                    else -> "Protection is ready."
                                },
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = openApps, modifier = Modifier.weight(1f), shape = RoundedCornerShape(17.dp)) {
                            Text("Protected apps")
                        }
                        Button(onClick = openSecurity, modifier = Modifier.weight(1f), shape = RoundedCornerShape(17.dp)) {
                            Text("Security")
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeStat("Protected", lockedCount.toString(), "apps", Modifier.weight(1f))
                HomeStat("PIN", if (pinSet) "ON" else "OFF", "required", Modifier.weight(1f))
                HomeStat("Service", if (serviceEnabled) "ON" else "OFF", "accessibility", Modifier.weight(1f))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Android safety check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Because AppGuard uses an AccessibilityService to detect protected apps, Android may require an additional confirmation for a sideloaded build.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open App info")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStat(title: String, value: String, detail: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppsTab(
    padding: PaddingValues,
    apps: List<LaunchableApp>,
    locked: Set<String>,
    pinStore: PinStore,
    onChanged: () -> Unit
) {
    var lockedOnly by rememberSaveable { mutableStateOf(false) }
    val shownApps = apps.filter { !lockedOnly || locked.contains(it.packageName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Text("Protected apps", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("${locked.size} selected", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { lockedOnly = false }, enabled = lockedOnly, shape = RoundedCornerShape(50.dp)) {
                    Text("All")
                }
                Button(onClick = { lockedOnly = true }, enabled = !lockedOnly, shape = RoundedCornerShape(50.dp)) {
                    Text("Locked")
                }
            }
        }

        items(shownApps, key = { it.packageName }) { app ->
            val checked = locked.contains(app.packageName)

            Card(shape = RoundedCornerShape(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            app.label.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            pinStore.setLocked(app.packageName, it)
                            onChanged()
                        }
                    )
                }
            }
        }

        if (shownApps.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Text(
                        "No launchable apps match this filter.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityTab(
    padding: PaddingValues,
    pinStore: PinStore,
    serviceEnabled: Boolean,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var biometric by rememberSaveable { mutableStateOf(pinStore.isBiometricEnabled()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Text("Security", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Authentication and protection status.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        if (serviceEnabled) "Protection is active" else "Protection needs setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (serviceEnabled) "The AppGuard AccessibilityService is enabled."
                        else "Enable AppGuard from Android Accessibility settings to lock apps.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open Accessibility settings")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Why Accessibility is required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "AppGuard uses Android Accessibility only to detect selected protected apps becoming active. It does not read their screen content or use the service to change security settings.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PIN protection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(12) },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it.filter(Char::isDigit).take(12) },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
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
                                    onChanged()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (pinStore.hasPin()) "Change PIN" else "Set PIN")
                    }

                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

                    HorizontalDivider()

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Biometric unlock", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Use fingerprint, face, or the device credential prompt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = biometric,
                            enabled = pinStore.hasPin() &&
                                runCatching { BiometricSupport.canAuthenticate(context) }.getOrDefault(false),
                            onCheckedChange = {
                                biometric = it
                                pinStore.setBiometricEnabled(it)
                                onChanged()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreTab(
    padding: PaddingValues,
    lockPreferences: LockPreferences,
    context: Context
) {
    var timing by rememberSaveable { mutableStateOf(lockPreferences.timing().key) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var screenOff by rememberSaveable { mutableStateOf(lockPreferences.lockOnScreenOff()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Text("More", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("App behavior and project information.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lock behavior", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                    Box {
                        Button(
                            onClick = { menuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(17.dp)
                        ) {
                            Text("Re-lock: " + LockTiming.fromKey(timing).label)
                        }

                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            LockTiming.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        timing = option.key
                                        lockPreferences.setTiming(option)
                                        menuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Lock when screen turns off", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Require authentication again after the display is locked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = screenOff,
                            onCheckedChange = {
                                screenOff = it
                                lockPreferences.setLockOnScreenOff(it)
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Project Maintainer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("LS", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Leon Sony", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Android & Linux developer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Jackson4Rocks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Jackson4Rocks"))
                                )
                            }.onFailure {
                                Toast.makeText(context, "Couldn't open GitHub", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("View GitHub profile")
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Protection controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Need to use a banking or other sensitive app without AppGuard's Accessibility service enabled? Pause protection manually here, then re-enable AppGuard from Android Accessibility settings when you're finished.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            context.sendBroadcast(
                                Intent(AppLockAccessibilityService.ACTION_PAUSE_PROTECTION)
                                    .setPackage(context.packageName)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Pause protection")
                    }

                    Spacer(Modifier.height(18.dp))
                    Text("App information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("AppGuard 0.3.0", color = MaterialTheme.colorScheme.primary)
                    Text("Local app locking with Android authentication APIs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return runCatching {
        pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .mapNotNull {
                runCatching {
                    LaunchableApp(it.packageName, pm.getApplicationLabel(it).toString())
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, AppLockAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabled.split(':').any {
        ComponentName.unflattenFromString(it) == expected
    }
}
