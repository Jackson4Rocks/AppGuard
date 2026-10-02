package dev.jackson4rocks.appguard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DashboardUi() }
    }
}

private enum class Tab { HOME, APPS, SETTINGS }

@Composable
private fun DashboardUi() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appguard_security", Context.MODE_PRIVATE) }
    val lockPrefs = remember { LockPreferences(context) }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME.name) }
    var refresh by rememberSaveable { mutableStateOf(0) }

    val apps = remember(refresh) {
        runCatching {
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            ).mapNotNull { it.activityInfo?.applicationInfo }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .map { it.packageName to context.packageManager.getApplicationLabel(it).toString() }
                .sortedBy { it.second.lowercase() }
        }.getOrDefault(emptyList())
    }

    val locked = remember(refresh) {
        prefs.getStringSet("locked_packages", emptySet())?.toSet() ?: emptySet()
    }

    BackHandler(enabled = tab != Tab.HOME.name) { tab = Tab.HOME.name }

    Scaffold(
        containerColor = Color(0xFF07130F),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0B1814)) {
                NavigationBarItem(
                    selected = tab == Tab.HOME.name,
                    onClick = { tab = Tab.HOME.name },
                    icon = { Text("⌂") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = tab == Tab.APPS.name,
                    onClick = { tab = Tab.APPS.name },
                    icon = { Text("▦") },
                    label = { Text("Apps") }
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS.name,
                    onClick = { tab = Tab.SETTINGS.name },
                    icon = { Text("⚙") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        when (Tab.valueOf(tab)) {
            Tab.HOME -> HomePage(padding, locked.size, { tab = Tab.APPS.name }, { tab = Tab.SETTINGS.name })
            Tab.APPS -> AppsPage(padding, apps, locked) { packageName, enabled ->
                val next = locked.toMutableSet()
                if (enabled) next.add(packageName) else next.remove(packageName)
                prefs.edit().putStringSet("locked_packages", next).apply()
                refresh++
            }
            Tab.SETTINGS -> SettingsPage(padding, lockPrefs, context)
        }
    }
}

@Composable
private fun HomePage(
    padding: PaddingValues,
    lockedCount: Int,
    openApps: () -> Unit,
    openSettings: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Text("AppGuard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("A calmer way to protect apps.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF07533B))
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(R.drawable.appguard_icon_vector),
                            null,
                            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (lockedCount == 0) "Ready to protect" else "${lockedCount} apps protected",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA9F8D3)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Choose protected apps from Apps, then finish Android's Accessibility setup.",
                                color = Color(0xFFA9F8D3)
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = openApps, modifier = Modifier.weight(1f)) { Text("Apps") }
                        Button(onClick = openSettings, modifier = Modifier.weight(1f)) { Text("Settings") }
                    }
                }
            }
        }

        item {
            Card(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Accessibility service", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Android may show an extra confirmation for sideloaded Accessibility Services. That warning is controlled by Android.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Open system settings", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AppsPage(
    padding: PaddingValues,
    apps: List<Pair<String, String>>,
    locked: Set<String>,
    onChange: (String, Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Text("Protected apps", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("${locked.size} selected", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(apps, key = { it.first }) { app ->
            val checked = locked.contains(app.first)
            Card(shape = RoundedCornerShape(22.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF07533B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(app.second.take(1).uppercase(), color = Color(0xFFA9F8D3), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.second, fontWeight = FontWeight.Medium)
                        Text(app.first, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = checked, onCheckedChange = { onChange(app.first, it) })
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    padding: PaddingValues,
    lockPrefs: LockPreferences,
    context: Context
) {
    var timing by rememberSaveable { mutableStateOf(lockPrefs.timing().key) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var screenOff by rememberSaveable { mutableStateOf(lockPrefs.lockOnScreenOff()) }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lock behavior", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Box {
                        Button(
                            onClick = { menuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
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
                                        lockPrefs.setTiming(option)
                                        menuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Lock when screen turns off", fontWeight = FontWeight.Medium)
                            Text(
                                "Re-lock protected apps after the display is locked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = screenOff,
                            onCheckedChange = {
                                screenOff = it
                                lockPrefs.setLockOnScreenOff(it)
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Security setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Use the existing security setup screen for the access code and biometric option.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { context.startActivity(Intent(context, MainActivity::class.java)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open security setup")
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
                            Modifier.size(76.dp).clip(CircleShape).background(Color(0xFF07533B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "LS",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFFA9F8D3),
                                fontWeight = FontWeight.Bold
                            )
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
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Jackson4Rocks")))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("View GitHub profile")
                    }
                }
            }
        }
    }
}
