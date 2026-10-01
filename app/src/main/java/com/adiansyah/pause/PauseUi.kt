package com.adiansyah.pause

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

private val Paper = Color(0xFFF7F8F1)
private val Forest = Color(0xFF173D36)
private val Citrus = Color(0xFFD7EF72)
private val Coral = Color(0xFFE9785B)
private val SoftGreen = Color(0xFFE6EDDF)
private val Muted = Color(0xFF64746D)
private val Lavender = Color(0xFFECE9FA)

@Composable
fun PauseApp(refreshTick: Int) {
    val context = LocalContext.current
    val store = remember { PauseStore(context) }
    var limits by remember(refreshTick) { mutableStateOf(store.limits()) }
    var page by remember { mutableStateOf("main") }
    val monitoringEnabled = remember(refreshTick) { isMonitorEnabled(context) }
    val apps = remember(refreshTick) { installedLaunchableApps(context) }
    val usedByPackage = remember(refreshTick, limits) {
        limits.keys.associateWith(store::usedMillis)
    }

    PauseTheme {
        when {
            page == "apps" -> AppPickerScreen(
                apps = apps,
                initialLimits = limits,
                onCancel = { page = "main" },
                onSave = { updated ->
                    store.saveLimits(updated)
                    limits = updated
                    page = "main"
                },
            )
            limits.isEmpty() -> WelcomeScreen(onChoose = { page = "apps" })
            !monitoringEnabled -> SetupScreen(
                onOpenSettings = { openAccessibilitySettings(context) },
                onEditApps = { page = "apps" },
            )
            else -> DashboardScreen(
                apps = apps,
                limits = limits,
                usedByPackage = usedByPackage,
                onEdit = { page = "apps" },
            )
        }
    }
}

@Composable
fun PauseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Forest,
            onPrimary = Color.White,
            secondary = Citrus,
            onSecondary = Forest,
            tertiary = Coral,
            background = Paper,
            onBackground = Forest,
            surface = Paper,
            onSurface = Forest,
            surfaceVariant = SoftGreen,
            onSurfaceVariant = Muted,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper, content = content)
    }
}

@Composable
private fun WelcomeScreen(onChoose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.focus_axolotl),
            contentDescription = null,
            modifier = Modifier.size(232.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Make room for your day.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Choose the apps that pull you in. Morrow gives you 15 seconds to reconsider before each one opens, then brings you home when its daily limit runs out.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = "Choose apps", onClick = onChoose)
    }
}

@Composable
private fun SetupScreen(onOpenSettings: () -> Unit, onEditApps: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("One last step", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Android needs Morrow’s Accessibility Service to add a 15-second pause before selected apps open and cover them when their daily limit runs out.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Morrow checks app changes only. It doesn’t read screen text or taps, and your usage stays on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = Forest,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = "Open Accessibility settings", onClick = onOpenSettings)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onEditApps) { Text("Change selected apps") }
        Spacer(Modifier.height(8.dp))
        Text(
            "Turn on “Morrow app blocker”, then come back here.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}

@Composable
private fun DashboardScreen(
    apps: List<LaunchableApp>,
    limits: Map<String, Int>,
    usedByPackage: Map<String, Long>,
    onEdit: () -> Unit,
) {
    val appByPackage = remember(apps) { apps.associateBy(LaunchableApp::packageName) }
    val rows = limits.toList().mapNotNull { (packageName, minutes) ->
        appByPackage[packageName]?.let { app -> app to UsageAllowance(usedByPackage[packageName] ?: 0L, minutes) }
    }
    val remainingMinutes = rows.sumOf { (_, allowance) ->
        ceil(allowance.remainingMillis / 60_000.0).toLong()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onEdit) { Text("Edit limits") }
        }
        Text(
            if (remainingMinutes == 1L) "1 minute left" else "$remainingMinutes minutes left",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
            fontWeight = FontWeight.Bold,
        )
        Text(
            "across ${rows.size} ${if (rows.size == 1) "app" else "apps"} today",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
        )
        Spacer(Modifier.height(28.dp))
        if (rows.isEmpty()) {
            Text("Add an app to start setting limits.", color = Muted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                items(rows, key = { it.first.packageName }) { (app, allowance) ->
                    AllowanceRow(app, allowance)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text("Change which apps are paused")
        }
    }
}

@Composable
private fun AllowanceRow(app: LaunchableApp, allowance: UsageAllowance) {
    val context = LocalContext.current
    val icon = remember(app.packageName) { appIcon(context, app.packageName) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon, app.label)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                val usedMinutes = allowance.usedMillis / 60_000L
                Text(
                    "$usedMinutes of ${allowance.limitMinutes} min used",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
            Text(
                if (allowance.isExhausted) "Paused" else "${allowance.remainingMillis / 60_000L} min left",
                color = if (allowance.isExhausted) Coral else Forest,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(10.dp))
        ProgressLine(allowance.progress)
    }
}

@Composable
private fun ProgressLine(progress: Float) {
    Canvas(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)) {
        drawRoundRect(color = SoftGreen)
        if (progress > 0f) {
            drawRoundRect(color = if (progress >= 0.9f) Coral else Citrus, size = size.copy(width = size.width * progress))
        }
    }
}

@Composable
private fun AppPickerScreen(
    apps: List<LaunchableApp>,
    initialLimits: Map<String, Int>,
    onCancel: () -> Unit,
    onSave: (Map<String, Int>) -> Unit,
) {
    var draft by remember(initialLimits) { mutableStateOf(initialLimits.toMap()) }
    var query by remember { mutableStateOf("") }
    var appToEdit by remember { mutableStateOf<LaunchableApp?>(null) }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Text("Choose apps", fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { onSave(draft) }) { Text("Save") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Pick the apps you want a limit for.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Each starts at 30 minutes a day. You can adjust it below.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Find an app") },
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filteredApps, key = LaunchableApp::packageName) { app ->
                val selected = app.packageName in draft
                val context = LocalContext.current
                val icon = remember(app.packageName) { appIcon(context, app.packageName) }
                Row(
                    modifier = Modifier.fillMaxWidth().height(66.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(icon, app.label)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        app.label,
                        modifier = Modifier.weight(1f).clickable {
                            draft = if (selected) draft - app.packageName else draft + (app.packageName to 30)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (selected) {
                        TextButton(onClick = { appToEdit = app }) {
                            Text("${draft.getValue(app.packageName)} min")
                        }
                    }
                    Checkbox(
                        checked = selected,
                        onCheckedChange = {
                            draft = if (it) draft + (app.packageName to 30) else draft - app.packageName
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = if (draft.isEmpty()) "Save choices" else "Continue with ${draft.size} ${if (draft.size == 1) "app" else "apps"}",
            onClick = { onSave(draft) },
        )
    }

    appToEdit?.let { app ->
        LimitDialog(
            app = app,
            initialMinutes = draft[app.packageName] ?: 30,
            onDismiss = { appToEdit = null },
            onSave = { minutes ->
                draft = draft + (app.packageName to minutes)
                appToEdit = null
            },
        )
    }
}

@Composable
private fun LimitDialog(
    app: LaunchableApp,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var minutes by remember(app.packageName, initialMinutes) { mutableFloatStateOf(initialMinutes.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily limit") },
        text = {
            Column {
                Text(app.label, color = Muted)
                Spacer(Modifier.height(16.dp))
                Text("${minutes.toInt()} minutes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = 5f..240f,
                    steps = 46,
                )
                Text("The limit resets at midnight.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(minutes.toInt()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Paper,
    )
}

@Composable
private fun AppIcon(icon: ImageBitmap?, label: String) {
    if (icon == null) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Lavender),
            contentAlignment = Alignment.Center,
        ) {
            Text(label.firstOrNull()?.uppercase() ?: "•", color = Forest, fontWeight = FontWeight.Bold)
        }
    } else {
        Image(icon, contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Forest),
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun BlockScreen(appLabel: String, onGoHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.focus_axolotl),
            contentDescription = null,
            modifier = Modifier.size(236.dp),
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "Your time in $appLabel is up.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your daily allowance resets at midnight.",
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = "Go home", onClick = onGoHome)
    }
}
