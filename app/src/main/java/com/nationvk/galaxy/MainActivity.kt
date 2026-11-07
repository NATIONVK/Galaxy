package com.nationvk.galaxy

import android.os.Bundle
import java.util.Locale
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import android.provider.Settings
import android.speech.tts.Voice
import android.speech.tts.TextToSpeech
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.content.pm.PackageManager
import android.content.Intent
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GalaxyApiProfiles.configureFromBuildConfig(this)
        val requestedTab = intent?.getStringExtra("galaxy_widget_tab")
        setContent { GalaxyApp(initialTab = requestedTab) }
    }
}

private val Space = Color(0xFF050611)
private val Panel = Color(0xFF101326)
private val Panel2 = Color(0xFF171B33)
private val Violet = Color(0xFF9B69FF)
private val Cyan = Color(0xFF55DFFF)
private val Muted = Color(0xFF929AB8)

private enum class GalaxyTab(val label: String, val glyph: String) {
    HOME("Home", "⌂"), CHAT("AI Chat", "✧"), LAB("Lab", "⌘"), MEMORY("Memory", "◈"), PROFILE("Profile", "◎"),
    VOICE("Voice", "♫"), RESEARCH("Research", "⌁"), CAMERA("Camera", "◉"), MODELS("AI Models", "⎇"),
    NOTIFICATIONS("Alerts", "♢"), SETTINGS("Settings", "⚙"), BLACKHOLE("Black Hole", "◉")
}

@Composable
fun GalaxyApp(initialTab: String? = null) {
    val startTab = when (initialTab?.uppercase(Locale.ROOT)) {
        "CHAT" -> GalaxyTab.CHAT
        "VOICE" -> GalaxyTab.VOICE
        "SETTINGS" -> GalaxyTab.SETTINGS
        "PROFILE" -> GalaxyTab.PROFILE
        else -> GalaxyTab.HOME
    }
    val navigationStack = remember {
        mutableStateListOf<GalaxyTab>().apply {
            add(GalaxyTab.HOME)
            if (startTab != GalaxyTab.HOME) add(startTab)
        }
    }
    val selected = navigationStack.last()

    fun navigate(tab: GalaxyTab) {
        if (navigationStack.lastOrNull() != tab) navigationStack.add(tab)
    }

    BackHandler(enabled = navigationStack.size > 1) {
        navigationStack.removeAt(navigationStack.lastIndex)
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Space) {
            Box(Modifier.fillMaxSize()) {
                StarBackdrop(Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize().imePadding()) {
                    OfflineStatusBanner()
                    if (selected != GalaxyTab.BLACKHOLE) GalaxyTopBar()
                    Box(Modifier.weight(1f)) {
                        when (selected) {
                            GalaxyTab.BLACKHOLE -> BlackHoleExperience(onBack = {
                                if (navigationStack.size > 1) navigationStack.removeAt(navigationStack.lastIndex)
                                else navigate(GalaxyTab.HOME)
                            })
                            GalaxyTab.HOME -> HomeScreen(onOpen = { navigate(it) })
                            GalaxyTab.CHAT -> ChatScreen()
                            GalaxyTab.LAB -> CyberLabScreen()
                            GalaxyTab.MEMORY -> MemoryVaultScreen()
                            GalaxyTab.PROFILE -> ProfileScreen(onSettings = { navigate(GalaxyTab.SETTINGS) })
                            GalaxyTab.VOICE -> VoiceStudioScreen()
                            GalaxyTab.RESEARCH -> ResearchHubScreen()
                            GalaxyTab.CAMERA -> VisionStudioScreen()
                            GalaxyTab.MODELS -> ModelControlScreen()
                            GalaxyTab.NOTIFICATIONS -> NotificationCenterScreen()
                            GalaxyTab.SETTINGS -> SettingsScreen()
                        }
                    }
                    if (selected != GalaxyTab.BLACKHOLE) {
                        BottomNavigation(selected = selected, onSelect = { navigate(it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GalaxyTopBar() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Color(0xFF242A58)))), contentAlignment = Alignment.Center) {
            Text("✦", color = Color.White, fontSize = 23.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("GALAXY", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text("PERSONAL AI UNIVERSE", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Box(Modifier.clip(CircleShape).background(Color(0xFF14283A)).padding(horizontal = 11.dp, vertical = 7.dp)) {
            Text("●  UI FINALIZATION", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeScreen(onOpen: (GalaxyTab) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WELCOME TO YOUR UNIVERSE", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp)
                Text("Hello, Explorer.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.clip(CircleShape).background(Panel2).padding(11.dp)) { Text("✧", color = Violet, fontSize = 21.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 390.dp).clip(RoundedCornerShape(25.dp)).semantics { contentDescription = "Open Black Hole experience"; role = Role.Button }.clickable { onOpen(GalaxyTab.BLACKHOLE) }) {
            GalaxyGLSurfaceViewHost(Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD050611)))).padding(18.dp)) {
                Column {
                    Text("SINGULARITY", color = Cyan, fontSize = 10.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Text("Explore the unknown.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionHeading("YOUR AI UNIVERSE", "Explore modules")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            ModuleCard("✧", "AI CHAT", "Talk with Galaxy", Modifier.weight(1f)) { onOpen(GalaxyTab.CHAT) }
            ModuleCard("◈", "MEMORY", "Your knowledge vault", Modifier.weight(1f)) { onOpen(GalaxyTab.MEMORY) }
        }
        Spacer(Modifier.height(20.dp))
        SectionHeading("MORE GALAXY MODULES", "Open a workspace")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            ModuleCard("♫", "VOICE STUDIO", "Voice assistant", Modifier.weight(1f)) { onOpen(GalaxyTab.VOICE) }
            ModuleCard("◉", "VISION", "Camera & images", Modifier.weight(1f)) { onOpen(GalaxyTab.CAMERA) }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            ModuleCard("⎇", "AI MODELS", "Provider control", Modifier.weight(1f)) { onOpen(GalaxyTab.MODELS) }
            ModuleCard("♢", "NOTIFICATIONS", "Activity inbox", Modifier.weight(1f)) { onOpen(GalaxyTab.NOTIFICATIONS) }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            ModuleCard("⚙", "SETTINGS", "App preferences", Modifier.weight(1f)) { onOpen(GalaxyTab.SETTINGS) }
            ModuleCard("◎", "PROFILE", "Your Galaxy space", Modifier.weight(1f)) { onOpen(GalaxyTab.PROFILE) }
        }
        Spacer(Modifier.height(20.dp))
        SectionHeading("SYSTEM STATUS", "Foundation phase")
        Spacer(Modifier.height(10.dp))
        StatusPanel()
        Spacer(Modifier.height(18.dp))
        SectionHeading("PHONE BRAIN", "Live device health snapshot")
        Spacer(Modifier.height(10.dp))
        DeviceMonitorPanel()
        Spacer(Modifier.height(18.dp))
        SectionHeading("PHONE CONTROL CENTER", "Quick actions • Android-safe shortcuts")
        Spacer(Modifier.height(10.dp))
        PhoneQuickActionsPanel()
        Spacer(Modifier.height(18.dp))
        Text("BUILT FOR CURIOSITY  •  NATIONVK", color = Color(0xFF606985), fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(14.dp))
    }
}

private data class PhoneQuickAction(
    val title: String,
    val subtitle: String,
    val glyph: String,
    val action: () -> Intent
)

@Composable
private fun PhoneQuickActionsPanel() {
    val context = LocalContext.current
    var lastAction by remember { mutableStateOf("কোনো action চালানো হয়নি।") }
    val actions = listOf(
        PhoneQuickAction("Calculator", "ক্যালকুলেটর খুলবে", "＋") {
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
        },
        PhoneQuickAction("Camera", "Camera app খুলবে", "◉") {
            Intent("android.media.action.IMAGE_CAPTURE")
        },
        PhoneQuickAction("Dialer", "কল করার আগে Dialer খুলবে", "☎") {
            Intent(Intent.ACTION_DIAL)
        },
        PhoneQuickAction("Wi-Fi", "Wi-Fi settings খুলবে", "⌁") {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        },
        PhoneQuickAction("Bluetooth", "Bluetooth settings খুলবে", "ᛒ") {
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        },
        PhoneQuickAction("Display", "Display settings খুলবে", "☼") {
            Intent(Settings.ACTION_DISPLAY_SETTINGS)
        },
        PhoneQuickAction("Location", "Location settings খুলবে", "⌖") {
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        },
        PhoneQuickAction("All Settings", "Android Settings খুলবে", "⚙") {
            Intent(Settings.ACTION_SETTINGS)
        }
    )

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(12.dp)) {
        Text("QUICK LAUNCH", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(9.dp))
        actions.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pair.forEach { action ->
                    Column(
                        Modifier.weight(1f).padding(bottom = 9.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Panel2)
                            .semantics { contentDescription = "${action.title}: ${action.subtitle}"; role = Role.Button }
                            .clickable {
                                try {
                                    context.startActivity(action.action().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    lastAction = "${action.title} খোলার নির্দেশ পাঠানো হয়েছে।"
                                } catch (_: Exception) {
                                    lastAction = "${action.title} খোলা যায়নি; ফোনে উপযুক্ত app আছে কি না পরীক্ষা কর।"
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(action.glyph, color = Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(action.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(action.subtitle, color = Muted, fontSize = 9.sp, lineHeight = 13.sp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text(lastAction, color = Color(0xFFB8D8E8), fontSize = 9.sp, lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
        Text("নোট: Dialer শুধু খোলে—কোনো কল নিজে থেকে করা হয় না। Wi-Fi/Bluetooth/Display action শুধু Android-এর Settings page খোলে; সরাসরি system toggle করে না।",
            color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

private data class GalaxyDeviceSnapshot(
    val device: String,
    val android: String,
    val battery: String,
    val memory: String,
    val storage: String,
    val network: String
)

private fun readGalaxyDeviceSnapshot(context: android.content.Context): GalaxyDeviceSnapshot {
    val batteryManager = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
    val reportedBattery = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val battery = if (reportedBattery in 0..100) "$reportedBattery%" else {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) "${(level * 100) / scale}%" else "Unavailable"
    }

    val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memoryInfo = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val totalRam = memoryInfo.totalMem.coerceAtLeast(1L)
    val usedRam = (totalRam - memoryInfo.availMem).coerceAtLeast(0L)
    fun size(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.0f MB", bytes / (1024.0 * 1024.0))
    }

    val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
    val totalStorage = stat.totalBytes.coerceAtLeast(1L)
    val usedStorage = (totalStorage - stat.availableBytes).coerceAtLeast(0L)
    val connectivity = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = if (android.os.Build.VERSION.SDK_INT >= 23) {
        val active = connectivity.activeNetwork
        val caps = active?.let { connectivity.getNetworkCapabilities(it) }
        when {
            caps == null -> "Offline"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi connected"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data connected"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet connected"
            else -> "Connected"
        }
    } else if (connectivity.activeNetworkInfo?.isConnected == true) "Connected" else "Offline"

    return GalaxyDeviceSnapshot(
        device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim(),
        android = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
        battery = battery,
        memory = "${size(usedRam)} / ${size(totalRam)} used",
        storage = "${size(usedStorage)} / ${size(totalStorage)} used",
        network = network
    )
}

@Composable
private fun DeviceMonitorPanel() {
    val context = LocalContext.current
    var snapshot by remember(context) { mutableStateOf(readGalaxyDeviceSnapshot(context)) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(snapshot.device, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(snapshot.android, color = Muted, fontSize = 9.sp)
            }
            Text("REFRESH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Violet)
                    .clickable { snapshot = readGalaxyDeviceSnapshot(context) }
                    .padding(horizontal = 12.dp, vertical = 9.dp))
        }
        Spacer(Modifier.height(12.dp))
        DeviceMetricRow("Battery", snapshot.battery, "▰")
        DeviceMetricRow("Memory (RAM)", snapshot.memory, "▦")
        DeviceMetricRow("Internal storage", snapshot.storage, "▤")
        DeviceMetricRow("Network", snapshot.network, "⌁")
        Text("ডিভাইসের তথ্য ফোন থেকেই পড়া হয়; এই panel কোনো তথ্য cloud-এ পাঠায় না। Refresh চাপলে নতুন snapshot নেওয়া হবে।",
            color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DeviceMetricRow(label: String, value: String, glyph: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(glyph, color = Cyan, fontSize = 14.sp, modifier = Modifier.width(26.dp))
        Text(label, color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModelControlScreen() {
    var mode by remember { mutableStateOf("AUTO") }
    var selectedModel by remember { mutableStateOf("Galaxy Default") }
    val models = listOf("Galaxy Default", "Fast Assistant", "Reasoning Model", "Vision-capable Model")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("INTELLIGENCE CONTROL", color = Cyan, fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("Model Control", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("একটি Galaxy Core, তোমার পছন্দমতো model selection.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(16.dp)) {
            Text("SELECTION MODE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionModeCard("AUTO", "Smart routing", "Galaxy chooses by task", mode == "AUTO", Modifier.weight(1f)) { mode = "AUTO" }
                SelectionModeCard("MANUAL", "Your choice", "You select the model", mode == "MANUAL", Modifier.weight(1f)) { mode = "MANUAL" }
            }
            Spacer(Modifier.height(12.dp))
            Text(if (mode == "AUTO") "Automatic mode will select an available model based on task type and capability. Routing activates after Core integration." else "Manual mode lets you choose a preferred model. The selection below is currently a UI preference preview.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("MODEL DIRECTORY", color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        models.forEachIndexed { index, model ->
            val active = selectedModel == model
            Row(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(16.dp)).background(if (active) Color(0xFF202447) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { if (mode == "MANUAL") selectedModel = model }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (active) Color(0x443FBCFF) else Panel2), contentAlignment = Alignment.Center) {
                    Text(listOf("✦", "⚡", "◈", "◉")[index], color = if (active) Cyan else Violet, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(model, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (index == 3) "Image and document tasks" else if (index == 2) "Complex multi-step tasks" else if (index == 1) "Quick everyday responses" else "Default assistant profile", color = Muted, fontSize = 10.sp)
                }
                Text(if (active) "SELECTED" else if (mode == "AUTO") "AUTO" else "CHOOSE", color = if (active) Cyan else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF14283A)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 10.sp)
            Spacer(Modifier.width(9.dp))
            Text("Demo interface only — no model provider is connected yet.", color = Color(0xFFB8D8E8), fontSize = 10.sp)
        }
    }
}

@Composable
private fun SelectionModeCard(title: String, heading: String, description: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(if (active) Color(0xFF25294F) else Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable(onClick = onClick).padding(12.dp)) {
        Text(if (active) "●  $title" else "○  $title", color = if (active) Cyan else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(heading, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun BlackHoleExperience(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("galaxy_preferences", android.content.Context.MODE_PRIVATE) }
    var zoom by remember { mutableFloatStateOf(prefs.getFloat("bh_zoom", 1.0f)) }
    val speed = prefs.getFloat("bh_speed", 1.0f)
    val intensity = prefs.getFloat("bh_intensity", 1.0f)
    val particles = prefs.getFloat("bh_particles", 0.65f)
    var glView by remember { mutableStateOf<GalaxyGLSurfaceView?>(null) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer { translationX = panOffset.x; translationY = panOffset.y }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.7f, 1.8f)
                        panOffset += pan
                        glView?.updateVisuals(zoom, speed, intensity, particles)
                    }
                },
            factory = { context -> GalaxyGLSurfaceView(context).also { glView = it } },
            update = { it.updateVisuals(zoom, speed, intensity, particles) }
        )
        Box(Modifier.fillMaxSize().padding(18.dp)) {
            Box(Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(30.dp)).background(Color(0xAA101326))
                .heightIn(min = 48.dp).semantics { role = Role.Button }.clickable(onClick = onBack).padding(horizontal = 16.dp, vertical = 11.dp)) {
                Text("←  BACK TO GALAXY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text("LIVE BLACK HOLE", Modifier.align(Alignment.TopEnd).padding(top = 16.dp), color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp)
            Text("Pinch to zoom · Drag to move", Modifier.align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(18.dp)).background(Color(0x99080B1B)).padding(horizontal = 14.dp, vertical = 9.dp),
                color = Muted, fontSize = 10.sp)
        }
    }
}
@Composable
private fun GalaxyGLSurfaceViewHost(modifier: Modifier) {
    AndroidView(modifier = modifier, factory = { context -> GalaxyGLSurfaceView(context) })
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f))
        Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun ModuleCard(icon: String, title: String, description: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(19.dp)).background(Brush.linearGradient(listOf(Panel2, Panel))).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable(onClick = onClick).padding(15.dp)) {
        Box(Modifier.size(35.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x332F9DFF)), contentAlignment = Alignment.Center) {
            Text(icon, color = Cyan, fontSize = 21.sp)
        }
        Spacer(Modifier.height(13.dp))
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(description, color = Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Text("OPEN  ↗", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun StatusPanel() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp)) {
        StatusRow("Visual interface", "In progress", Cyan)
        StatusRow("3D environment", "Prototype", Violet)
        StatusRow("AI core & memory", "Coming next", Muted)
        StatusRow("Cloud services", "Not connected", Muted)
    }
}

@Composable
private fun StatusRow(name: String, status: String, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
        Spacer(Modifier.width(10.dp))
        Text(name, color = Color(0xFFD8DDF0), fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(status, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChatScreen() {
    val context = LocalContext.current
    var providerReady by remember { mutableStateOf((GalaxyApiProfiles.hasEnabled(context) || GalaxyAiProviderStore.load(context).isConfigured)) }
    var isSending by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(GalaxyLocalConversationStore.load(context)) }
    var attachmentOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var contextInfo by remember { mutableStateOf(GalaxyConversationContext.prepare(context, messages, "")) }
    var showContextDetails by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(GalaxyLocalConversationStore.exportJson(context))
                }
                notice = "Conversation backup exported as JSON."
            } catch (_: Exception) {
                notice = "Export failed. Check available device storage and try again."
            }
        }
    }
    val visibleMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages else messages.filter { it.second.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GALAXY CONVERSATION", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("AI Chat", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (providerReady) "AI CORE READY" else "OFFLINE MODE", color = if (providerReady) Cyan else Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("EXPORT JSON", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).clickable {
                        exportLauncher.launch("galaxy-chat-backup.json")
                    }.padding(horizontal = 9.dp, vertical = 7.dp))
            }
        }
        if (messages.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = Cyan, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).padding(vertical = 9.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                    decorationBox = { inner -> Box { if (searchQuery.isEmpty()) Text("Search saved messages...", color = Muted, fontSize = 11.sp); inner() } }
                )
                if (searchQuery.isNotEmpty()) Text("CLEAR", color = Muted, fontSize = 9.sp,
                    modifier = Modifier.clickable { searchQuery = "" }.padding(5.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = Cyan, fontSize = 18.sp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Galaxy Assistant", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                val personalization = GalaxyPersonalizationStore.load(context)
                Text("Preview • ${personalization.assistantMode} mode • ${personalization.responseStyle} • ${personalization.tone}", color = Muted, fontSize = 10.sp)
            }
            Text(if (providerReady) "● READY" else "● OFFLINE", color = if (providerReady) Cyan else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).clickable { showContextDetails = !showContextDetails }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◈", color = Cyan, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("CONVERSATION CONTEXT", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("${contextInfo.turnCount} recent turns • ${contextInfo.memoryCount} relevant memories • ${if (contextInfo.isFollowUp) "follow-up detected" else "context ready"}", color = Muted, fontSize = 10.sp)
                }
                Text(if (showContextDetails) "HIDE" else "DETAILS", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (showContextDetails) {
                Spacer(Modifier.height(8.dp))
                Text(contextInfo.preview, color = Color(0xFFD8DDF0), fontSize = 10.sp, lineHeight = 15.sp)
                Text("Context is prepared locally for a future connected AI provider. No AI reply is generated in this build.", color = Muted, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        if (messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.radialGradient(listOf(Color(0xFF24234D), Panel, Space))).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0x332F9DFF)), contentAlignment = Alignment.Center) {
                    Text("✧", color = Cyan, fontSize = 42.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text("Where should we begin?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("তোমার ভাবনা লিখে রাখো। AI Core যুক্ত হলে এখানেই বাস্তব উত্তর পাবে।", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(20.dp))
                listOf("Explain a statistics concept", "Help me plan a research project", "Explore a Python idea").forEach { suggestion ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { draft = suggestion }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✧", color = Violet, fontSize = 13.sp, modifier = Modifier.semantics { contentDescription = "Suggested prompt" })
                        Spacer(Modifier.width(9.dp))
                        Text(suggestion, color = Color(0xFFDCE2F7), fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text("↗", color = Cyan, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 5.dp)) {
                if (visibleMessages.isEmpty()) {
                    Text("No matching saved messages.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                }
                visibleMessages.forEach { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.first) Arrangement.End else Arrangement.Start) {
                        Column(Modifier.widthIn(max = 290.dp).padding(vertical = 5.dp).clip(RoundedCornerShape(17.dp)).background(if (message.first) Color(0xFF29264F) else Panel).padding(13.dp)) {
                            Text(if (message.first) "YOU" else "GALAXY", color = if (message.first) Cyan else Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(message.second, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
                Text("Messages are stored locally on this device. Search and JSON export work offline; no AI reply is generated in demo mode.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
        if (attachmentOpen) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(13.dp)).background(Panel2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("＋", color = Cyan, fontSize = 18.sp, modifier = Modifier.semantics { contentDescription = "Attachment options" })
                Spacer(Modifier.width(8.dp))
                Text("Attachments will be enabled with file and vision services.", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("CLOSE", color = Cyan, fontSize = 10.sp, modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { attachmentOpen = false })
            }
        }
        if (notice.isNotBlank()) {
            Text(notice, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 5.dp, bottom = 6.dp))
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel2).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Panel).semantics { contentDescription = if (attachmentOpen) "Close attachment options" else "Open attachment options"; role = Role.Button }.clickable { attachmentOpen = !attachmentOpen }, contentAlignment = Alignment.Center) {
                Text("＋", color = Cyan, fontSize = 21.sp)
            }
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = draft,
                onValueChange = { draft = it; notice = "" },
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) Text("Write a message...", color = Muted, fontSize = 12.sp)
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(7.dp))
            Box(Modifier.size(48.dp).clip(CircleShape).background(if (draft.isBlank() || isSending) Panel else Violet).semantics { contentDescription = "Send message"; role = Role.Button }.clickable(enabled = draft.isNotBlank() && !isSending) {
                val value = draft.trim()
                if (value.isNotEmpty()) {
                    contextInfo = GalaxyConversationContext.prepare(context, messages, value)
                    val detectedFollowUp = contextInfo.isFollowUp
                    val updated = messages + (true to value)
                    if (GalaxyLocalConversationStore.save(context, updated)) {
                        messages = updated
                        draft = ""
                        providerReady = GalaxyApiProfiles.hasEnabled(context) || GalaxyAiProviderStore.load(context).isConfigured
                        if (providerReady) {
                            isSending = true
                            notice = "Galaxy is preparing a secure AI request…"
                            GalaxyAiClient.send(context, contextInfo.promptContext, contextInfo.resolvedMessage) { result ->
                                isSending = false
                                result.fold(
                                    { answer ->
                                        val completed = messages + (false to answer)
                                        if (GalaxyLocalConversationStore.save(context, completed)) {
                                            messages = completed
                                            contextInfo = GalaxyConversationContext.prepare(context, completed, "")
                                            notice = "AI response received and saved locally."
                                        } else {
                                            notice = "AI response received, but saving the reply failed."
                                        }
                                    },
                                    { error ->
                                        notice = "AI request failed: ${error.message ?: "Unknown error"}. Your message is still saved."
                                    }
                                )
                            }
                        } else {
                            notice = if (detectedFollowUp) "Message saved. Follow-up context prepared locally. Configure a provider in Settings for live AI replies." else "Message saved locally. Configure an AI provider in Settings to receive live replies."
                        }
                    } else {
                        notice = "Could not save this message. Please try again."
                    }
                }
            }, contentAlignment = Alignment.Center) {
                Text(if (isSending) "…" else "↑", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (isSending) "Galaxy is thinking…" else if (providerReady) "Live AI • provider configured" else "Offline • local notes only", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text("CLEAR CHAT", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable(enabled = !isSending) {
                if (GalaxyLocalConversationStore.clear(context)) {
                    messages = emptyList()
                    contextInfo = GalaxyConversationContext.prepare(context, emptyList(), "")
                    notice = "Local conversation cleared."
                } else {
                    notice = "Could not clear local conversation."
                }
            }.padding(5.dp))
        }
    }
}

@Composable
private fun VoiceStudioScreen() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("galaxy_preferences", android.content.Context.MODE_PRIVATE) }
    var selectedVoice by remember { mutableStateOf(preferences.getString("voice_gender", "Male") ?: "Male") }
    var recognitionLanguage by remember { mutableStateOf("bn-BD") }
    var transcript by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready — tap Start and say “Hey Galaxy”") }
    var listening by remember { mutableStateOf(false) }
    var recognitionSession by remember { mutableIntStateOf(0) }
    var speed by remember { mutableFloatStateOf(1f) }
    var ttsReady by remember { mutableStateOf(false) }
    var pendingBackgroundStart by remember { mutableStateOf(false) }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    val recognizer = remember { mutableStateOf<SpeechRecognizer?>(null) }
    val notificationPermissionRequired = android.os.Build.VERSION.SDK_INT >= 33
    val notificationPermissionGranted = !notificationPermissionRequired ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (pendingBackgroundStart) {
            pendingBackgroundStart = false
            try {
                ContextCompat.startForegroundService(context, Intent(context, GalaxyVoiceWakeService::class.java))
                status = if (granted) "Background listening চালু হয়েছে। Notification থেকে status দেখতে পারবি।"
                    else "Background listening চালু হয়েছে; notification permission না থাকায় notification drawer-এ status নাও দেখা যেতে পারে।"
            } catch (error: Exception) {
                status = "Background service চালু হয়নি: ${error.message ?: "অজানা সমস্যা"}"
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            status = if (pendingBackgroundStart) "Microphone allowed. Notification permission যাচাই হচ্ছে…" else "Microphone allowed. আবার START LISTENING চাপ।"
            if (pendingBackgroundStart) {
                if (notificationPermissionRequired && !notificationPermissionGranted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    pendingBackgroundStart = false
                    try {
                        ContextCompat.startForegroundService(context, Intent(context, GalaxyVoiceWakeService::class.java))
                        status = "Background listening চালু হয়েছে।"
                    } catch (error: Exception) {
                        status = "Background service চালু হয়নি: ${error.message ?: "অজানা সমস্যা"}"
                    }
                }
            }
        } else {
            pendingBackgroundStart = false
            status = "Microphone permission denied. Android App Settings থেকে permission দিলে Voice চালু হবে।"
        }
    }

    fun requestBackgroundListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingBackgroundStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (notificationPermissionRequired && !notificationPermissionGranted) {
            pendingBackgroundStart = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        try {
            ContextCompat.startForegroundService(context, Intent(context, GalaxyVoiceWakeService::class.java))
            status = "Background listening চালু হয়েছে। Notification থেকে status দেখতে পারবি।"
        } catch (error: Exception) {
            status = "Background service চালু হয়নি: ${error.message ?: "অজানা সমস্যা"}"
        }
    }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
            if (result == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale("bn", "BD")
                tts.value?.setSpeechRate(speed)
            }
        }
        tts.value = engine
        onDispose {
            recognitionSession++
            listening = false
            recognizer.value?.destroy()
            recognizer.value = null
            engine.stop()
            engine.shutdown()
            tts.value = null
        }
    }

    fun speak(text: String) {
        val engine = tts.value ?: return
        val voices = engine.voices ?: emptySet<Voice>()
        val wanted = if (selectedVoice == "Male") "male" else "female"
        val match = voices.firstOrNull { it.name.contains(wanted, ignoreCase = true) }
        if (match != null) engine.voice = match
        val bn = engine.setLanguage(Locale("bn", "BD"))
        if (bn == TextToSpeech.LANG_MISSING_DATA || bn == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(speed)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "galaxy_reply")
    }

    fun runCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
            .replace("hey galaxy", "").replace("hey, galaxy", "")
            .replace("হেই গ্যালাক্সি", "").replace("হে গ্যালাক্সি", "")
            .trim()
        transcript = raw
        when {
            command.startsWith("search ") || command.startsWith("সার্চ ") || command.startsWith("খুঁজে দাও ") -> {
                val query = command.substringAfter(" ").trim()
                if (query.isBlank()) {
                    status = "কী search করতে চাও বলো।"
                    speak("কী খুঁজব বলো")
                } else {
                    try {
                        val searchUri = android.net.Uri.parse(
                            "https://www.google.com/search?q=" + android.net.Uri.encode(query)
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, searchUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        status = "Web search শুরু হয়েছে: $query"
                        speak("$query search করছি")
                    } catch (error: Exception) {
                        status = "Browser দিয়ে search করা যায়নি।"
                        speak("সার্চ করা যাচ্ছে না")
                    }
                }
            }
            command.contains("calculator") || command.contains("ক্যালকুলেটর") -> {
                try {
                    val calculatorPackages = listOf(
                        "com.google.android.calculator",
                        "com.android.calculator2",
                        "com.sec.android.app.popupcalculator"
                    )
                    val calculatorIntent = calculatorPackages.firstNotNullOfOrNull {
                        context.packageManager.getLaunchIntentForPackage(it)
                    } ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
                    context.startActivity(calculatorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    status = "Calculator খোলা হচ্ছে"
                    speak("ক্যালকুলেটর খুলছি")
                } catch (error: Exception) {
                    status = "Calculator app পাওয়া যায়নি।"
                    speak("ক্যালকুলেটর খোলা যাচ্ছে না")
                }
            }
            command.contains("gallery") || command.contains("গ্যালারি") || command.contains("ছবি দেখাও") -> {
                try {
                    val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                    context.startActivity(Intent.createChooser(galleryIntent, "Choose an image").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    status = "Gallery picker খোলা হচ্ছে"
                    speak("গ্যালারি খুলছি")
                } catch (error: Exception) {
                    status = "Gallery খোলা যায়নি।"
                    speak("গ্যালারি খোলা যাচ্ছে না")
                }
            }
            command.contains("youtube") || command.contains("ইউটিউব") -> {
                val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (launch != null) context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                else context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                status = "YouTube খোলার নির্দেশ দেওয়া হয়েছে"
                speak("YouTube খুলছি")
            }
            command.contains("wifi") || command.contains("wi-fi") || command.contains("ওয়াইফাই") || command.contains("ওয়াইফাই") -> {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                status = "Wi-Fi settings খোলা হচ্ছে"
                speak("Wi-Fi settings খুলছি")
            }
            command.contains("bluetooth") || command.contains("ব্লুটুথ") -> {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                status = "Bluetooth settings খোলা হচ্ছে"
                speak("Bluetooth settings খুলছি")
            }
            command.contains("camera") || command.contains("ক্যামেরা") -> {
                try {
                    context.startActivity(Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    status = "Camera খোলা হচ্ছে"
                    speak("ক্যামেরা খুলছি")
                } catch (error: Exception) {
                    status = "Camera app পাওয়া যায়নি: ${error.message ?: "অজানা সমস্যা"}"
                    speak("ক্যামেরা খোলা যাচ্ছে না")
                }
            }
            command.contains("dialer") || command.contains("phone") || command.contains("ডায়ালার") || command.contains("ফোন") -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    status = "Phone dialer খোলা হচ্ছে"
                    speak("ফোন ডায়ালার খুলছি")
                } catch (error: Exception) {
                    status = "Dialer খোলা যায়নি"
                    speak("ডায়ালার খোলা যাচ্ছে না")
                }
            }
            command.contains("browser") || command.contains("ইন্টারনেট") || command.contains("ব্রাউজার") -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    status = "Browser খোলা হচ্ছে"
                    speak("ব্রাউজার খুলছি")
                } catch (error: Exception) {
                    status = "Browser খোলা যায়নি"
                    speak("ব্রাউজার খোলা যাচ্ছে না")
                }
            }
            command.contains("map") || command.contains("ম্যাপ") || command.contains("মানচিত্র") -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=nearby places")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    status = "Maps খোলা হচ্ছে"
                    speak("ম্যাপ খুলছি")
                } catch (error: Exception) {
                    status = "Maps app পাওয়া যায়নি"
                    speak("ম্যাপ খোলা যাচ্ছে না")
                }
            }
            command.contains("settings") || command.contains("সেটিংস") -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                status = "Settings খোলা হচ্ছে"
                speak("Settings খুলছি")
            }
            command.isBlank() -> {
                status = "Wake phrase শুনেছি। এখন command বল।"
                speak("জি, বলুন।")
            }
            else -> {
                status = "Command চিনেছি: $command — এই action এখনো যোগ করা হয়নি"
                speak("তোমার নির্দেশটি বুঝেছি। এই কাজটি এখনো শেখানো হয়নি।")
            }
        }
    }

    fun startListening() {
        if (listening) {
            status = "ইতিমধ্যে শুনছি। নতুন session শুরু করতে আগে Stop চাপ।"
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            status = "এই ডিভাইসে Speech Recognition service পাওয়া যায়নি"
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val session = recognitionSession + 1
        recognitionSession = session
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.value?.destroy()
        recognizer.value = sr
        listening = true
        status = "শুনছি… “Hey Galaxy” বল"
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (session != recognitionSession) return; status = "শুনছি… Hey Galaxy" }
            override fun onBeginningOfSpeech() { if (session != recognitionSession) return; status = "কথা শনাক্ত করছি…" }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { if (session != recognitionSession) return; status = "কথা বিশ্লেষণ করছি…" }
            override fun onError(error: Int) {
                if (session != recognitionSession) return
                listening = false
                status = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio পাওয়া যায়নি। Microphone permission ও অন্য recording app পরীক্ষা কর।"
                    SpeechRecognizer.ERROR_CLIENT -> "Speech service বন্ধ হয়েছে। আবার Start চাপ।"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission নেই। Settings থেকে permission Allow কর।"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network সমস্যা। Internet সংযোগ পরীক্ষা কর।"
                    SpeechRecognizer.ERROR_NO_MATCH -> "কথা বোঝা যায়নি। একটু স্পষ্ট করে আবার বল।"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service ব্যস্ত। কয়েক সেকেন্ড পরে আবার চেষ্টা কর।"
                    SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error। পরে আবার চেষ্টা কর।"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি। Start চাপার পর কথা বল।"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "বাংলা (বাংলাদেশ) recognition এই service-এ পাওয়া যাচ্ছে না।"
                    else -> "Speech recognition error (code $error)। আবার চেষ্টা কর।"
                }
                recognizer.value?.destroy()
                recognizer.value = null
            }
            override fun onResults(results: Bundle?) {
                if (session != recognitionSession) return
                listening = false
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                transcript = heard
                if (heard.isBlank()) {
                    status = "কোনো কথা শনাক্ত হয়নি। আবার চেষ্টা কর।"
                } else {
                    // Voice Studio is already manually armed by the Start button,
                    // so it must accept a direct command without requiring a wake phrase.
                    runCommand(heard)
                }
                recognizer.value?.destroy()
                recognizer.value = null
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (session != recognitionSession) return
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) status = "শুনছি: $partial"
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recognitionLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Hey Galaxy / হেই গ্যালাক্সি বলুন")
        }
        try {
            sr.startListening(intent)
        } catch (error: SecurityException) {
            listening = false
            status = "Microphone permission পাওয়া যায়নি। Android Settings থেকে permission পরীক্ষা কর।"
            sr.destroy()
            recognizer.value = null
        } catch (error: IllegalStateException) {
            listening = false
            status = "Speech service এখন প্রস্তুত নয়। একটু পরে আবার চেষ্টা কর।"
            sr.destroy()
            recognizer.value = null
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GALAXY VOICE ASSISTANT", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Voice Studio", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("বাংলা + English • On-device Android voice services", color = Muted, fontSize = 11.sp)
            }
            Text("LOCAL", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(Panel2).padding(horizontal = 10.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.radialGradient(listOf(Color(0xFF34306C), Panel, Space))).padding(vertical = 24.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(150.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0x6655DFFF), Color(0x449B69FF), Color(0x11101326)))), contentAlignment = Alignment.Center) {
                Text(if (listening) "◉" else "♫", color = Cyan, fontSize = 54.sp)
            }
            Spacer(Modifier.height(15.dp))
            Text(if (listening) "Listening…" else "Hey Galaxy", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(status, color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.clip(RoundedCornerShape(14.dp)).background(if (listening) Color(0xFF542B3A) else Color(0xFF173B45)).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { if (!listening) startListening() }.padding(horizontal = 18.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (listening) "LISTENING" else "START LISTENING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable {
                    recognitionSession++
                    listening = false
                    recognizer.value?.cancel()
                    recognizer.value?.destroy()
                    recognizer.value = null
                    status = "Voice paused"
                }.padding(horizontal = 18.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STOP", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF173B45)).heightIn(min = 54.dp).semantics { role = Role.Button }.clickable {
                requestBackgroundListening()
            }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("START BACKGROUND", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text("Across apps • persistent notification", color = Color(0xFFB8D8E8), fontSize = 9.sp)
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF542B3A)).heightIn(min = 54.dp).semantics { role = Role.Button }.clickable {
                context.startService(Intent(context, GalaxyVoiceWakeService::class.java).setAction(GalaxyVoiceWakeService.ACTION_STOP))
                status = "Background listening stopped"
            }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("STOP BACKGROUND", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text("Microphone released", color = Color(0xFFE8B8C8), fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
            Text("VOICE COMMAND GUIDE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("বাংলা বা English-এ বলতে পারো:", color = Muted, fontSize = 10.sp)
            listOf(
                "“Search আজকের আবহাওয়া”  •  “সার্চ ঢাকা বিশ্ববিদ্যালয়”",
                "“Open calculator”  •  “গ্যালারি খুলে দাও”",
                "“Open YouTube”  •  “Wi-Fi settings”",
                "“Open camera”  •  “Open maps”  •  “Open settings”"
            ).forEach { example ->
                Text(example, color = Color(0xFFD8DDF0), fontSize = 10.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp))
            }
            Text("Voice command চালাতে START LISTENING চাপো; background mode আলাদাভাবে চালু করতে হবে।", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(14.dp))
        VoiceCommandHistoryPanel()
        Spacer(Modifier.height(16.dp))
        Text("VOICE TYPE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Male", "Female").forEach { voice ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (selectedVoice == voice) Color(0xFF29264F) else Panel2).heightIn(min = 52.dp).semantics { role = Role.Button }.clickable { selectedVoice = voice; preferences.edit().putString("voice_gender", voice).apply(); speak("আমি Galaxy। কীভাবে সাহায্য করতে পারি?") }.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (selectedVoice == voice) "● $voice" else voice, color = if (selectedVoice == voice) Cyan else Color.White, fontWeight = FontWeight.Bold)
                    Text(if (voice == "Male") "পুরুষ কণ্ঠ" else "নারী কণ্ঠ", color = Muted, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("RECOGNITION LANGUAGE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("Speech-to-text ভাষা নির্বাচন কর", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("bn-BD" to "বাংলা", "en-US" to "English").forEach { (code, label) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(if (recognitionLanguage == code) Color(0xFF29264F) else Panel2).semantics { role = Role.Button; selected = recognitionLanguage == code }.clickable {
                    recognitionLanguage = code
                    if (listening) {
                        recognitionSession++
                        listening = false
                        recognizer.value?.cancel()
                        recognizer.value?.destroy()
                        recognizer.value = null
                        status = "$label selected. আবার Start চাপ।"
                    } else status = "$label recognition selected"
                }.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (recognitionLanguage == code) "● $label" else label, color = if (recognitionLanguage == code) Cyan else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(if (code == "bn-BD") "Bangladesh" else "United States", color = Muted, fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
            Text("SPEECH SPEED", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.7f..1.4f)
            Text("Speed: ${String.format("%.1f", speed)}×", color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(8.dp))
            Text("RECOGNIZED SPEECH", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(transcript.ifBlank { "এখনো কোনো কথা শনাক্ত হয়নি" }, color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel2).padding(12.dp)) {
                Text("Supported starter commands", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("• Hey Galaxy, open YouTube\n• Hey Galaxy, Wi-Fi settings\n• Hey Galaxy, Bluetooth settings\n• Hey Galaxy, open Settings\n• Hey Galaxy, open Camera\n• Hey Galaxy, open Phone / Dialer\n• Hey Galaxy, open Browser\n• Hey Galaxy, open Maps", color = Muted, fontSize = 11.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Wake phrase currently works while Voice Studio is open and listening. Background / always-on wake word requires a separate Android foreground microphone service, visible notification and battery testing.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun MemoryVaultScreen() {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }
    var noteDraft by remember { mutableStateOf("") }
    var memoryPriority by remember { mutableIntStateOf(2) }
    var notes by remember { mutableStateOf(GalaxyMemoryStore.getAll(context)) }
    var trashItems by remember { mutableStateOf(GalaxyMemoryStore.getTrash(context)) }
    var snapshots by remember { mutableStateOf(GalaxyMemoryStore.getSnapshots(context)) }
    var showBackupCenter by remember { mutableStateOf(false) }
    var historyMemoryId by remember { mutableStateOf<Long?>(null) }
    var preferenceSuggestions by remember { mutableStateOf(GalaxyPreferenceLearner.suggestions(context)) }
    var showEditor by remember { mutableStateOf(false) }
    var editingMemoryId by remember { mutableStateOf<Long?>(null) }
    var notice by remember { mutableStateOf("") }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("ফাইলটি পড়া যায়নি।")
                if (notes.isNotEmpty()) GalaxyMemoryStore.createSnapshot(context)
                val result = GalaxyMemoryStore.importJson(context, json)
                notes = GalaxyMemoryStore.getAll(context)
                snapshots = GalaxyMemoryStore.getSnapshots(context)
                notice = "Import complete: ${result.imported} added, ${result.skippedDuplicates} duplicates skipped, ${result.rejected} invalid skipped. A pre-import restore point was saved when possible."
            } catch (error: Exception) {
                notice = "Import failed: " + (error.message ?: "Unknown error")
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val backup = GalaxyMemoryStore.exportJson(context)
                context.contentResolver.openOutputStream(uri)?.use { it.write(backup.toByteArray(Charsets.UTF_8)) }
                    ?: throw IllegalStateException("ফাইলে লেখা যায়নি।")
                notice = "Backup file saved successfully."
            } catch (error: Exception) {
                notice = "Export failed: " + (error.message ?: "Unknown error")
            }
        }
    }
    val recoveryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val recovery = GalaxyMemoryStore.exportRecoveryBackup(context)
                    ?: throw IllegalStateException("কোনো recovery copy পাওয়া যায়নি।")
                context.contentResolver.openOutputStream(uri)?.use { it.write(recovery.toByteArray(Charsets.UTF_8)) }
                    ?: throw IllegalStateException("Recovery file-এ লেখা যায়নি।")
                notice = "Recovery copy exported. Keep it safe before attempting any repair."
            } catch (error: Exception) {
                notice = "Recovery export failed: " + (error.message ?: "Unknown error")
            }
        }
    }
    val recoveryAvailable = GalaxyMemoryStore.hasRecoveryBackup(context)
    val memoryInsights = GalaxyMemoryStore.insights(context)
    val memoryReview = remember(notes) { GalaxyMemoryIntelligence.review(notes) }
    val categories = listOf("ALL", "PERSONAL", "PROJECT", "TASK", "CONVERSATION", "IDEAS", "RESEARCH", "LEARNING", "NOTES")
    val filtered = if (search.isBlank()) {
        notes.filter { selectedCategory == "ALL" || it.category == selectedCategory }
    } else {
        GalaxyMemoryStore.search(context, search, category = selectedCategory.takeIf { it != "ALL" }, limit = 1000)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GALAXY LONG-TERM MEMORY", color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("Memory Vault", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("তথ্য, পছন্দ, project ও task — এই ডিভাইসে সংরক্ষিত।", color = Muted, fontSize = 10.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("◈", color = Violet, fontSize = 23.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MEMORY INTELLIGENCE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("${memoryInsights.total} stored memories", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("LOCAL ONLY", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("PERSONAL", "PROJECT", "LEARNING", "RESEARCH").forEach { category ->
                    val count = memoryInsights.categoryCounts[category] ?: 0
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(Panel2).padding(7.dp)) {
                        Text(count.toString(), color = Cyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(category, color = Muted, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Text("Search now ranks matching memories by relevance, phrase match and recency. Memories stay on this device.", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (memoryReview.staleMemories.isNotEmpty() || memoryReview.conflicts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel2).padding(12.dp)) {
                Text("MEMORY HEALTH REVIEW", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("${memoryReview.staleMemories.size} possibly outdated • ${memoryReview.conflicts.size} possible conflicts", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                Text("এগুলো review hint মাত্র। Galaxy নিজে থেকে কোনো memory delete বা merge করবে না। High-priority memories protected.", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
                memoryReview.conflicts.take(3).forEach { conflict ->
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).background(Panel).padding(9.dp)) {
                        Text("POSSIBLE CONTRADICTION • shared: ${conflict.sharedTerms.joinToString(", ")}", color = Color(0xFFFFC28A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("A: ${conflict.first.content.take(110)}", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                        Text("B: ${conflict.second.content.take(110)}", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                        Text("EDIT FIRST MEMORY", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                editingMemoryId = conflict.first.id
                                selectedCategory = conflict.first.category
                                noteDraft = conflict.first.content
                                memoryPriority = conflict.first.priority
                                showEditor = true
                            }.padding(top = 6.dp))
                        Text("EDIT SECOND MEMORY", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                editingMemoryId = conflict.second.id
                                selectedCategory = conflict.second.category
                                noteDraft = conflict.second.content
                                memoryPriority = conflict.second.priority
                                showEditor = true
                            }.padding(top = 5.dp))
                    }
                }
                memoryReview.staleMemories.take(3).forEach { stale ->
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("OUTDATED?  ${stale.category}: ${stale.content.take(65)}", color = Muted, fontSize = 8.sp, modifier = Modifier.weight(1f))
                        Text("REVIEW", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                editingMemoryId = stale.id
                                selectedCategory = stale.category
                                noteDraft = stale.content
                                memoryPriority = stale.priority
                                showEditor = true
                            }.padding(start = 7.dp))
                    }
                }
            }
        }
        if (preferenceSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel2).padding(12.dp)) {
                Text("PREFERENCE LEARNING • REVIEW REQUIRED", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Saved PERSONAL memories থেকে সম্ভাব্য পছন্দ শনাক্ত হয়েছে। অনুমোদন না দিলে এগুলো profile-এ যোগ হবে না।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 5.dp))
                preferenceSuggestions.forEach { suggestion ->
                    Column(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(10.dp)).background(Panel).padding(9.dp)) {
                        Text(suggestion.statement, color = Color.White, fontSize = 10.sp, lineHeight = 15.sp)
                        Text("Confidence estimate: ${(suggestion.confidence * 100).toInt()}% • heuristic, not verified", color = Muted, fontSize = 8.sp, modifier = Modifier.padding(top = 4.dp))
                        Text("APPROVE & SAVE AS PREFERENCE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val approved = GalaxyPreferenceLearner.approve(context, suggestion)
                                notes = GalaxyMemoryStore.getAll(context)
                                preferenceSuggestions = GalaxyPreferenceLearner.suggestions(context).filterNot { it.sourceMemoryId == suggestion.sourceMemoryId }
                                notice = if (approved) "Preference approved and saved locally." else "Preference already exists or could not be saved."
                            }.padding(top = 7.dp))
                    }
                }
            }
        }
        if (recoveryAvailable) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF3A2630)).padding(12.dp)) {
                Text("RECOVERY COPY AVAILABLE", color = Color(0xFFFFC2A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Galaxy একটি damaged memory file শনাক্ত করেছে। মূল raw data overwrite না করে আলাদা recovery copy রাখা হয়েছে। আগে সেটি export করে রাখ।", color = Color.White, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 5.dp))
                Text("EXPORT RECOVERY COPY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF704052)).clickable { recoveryLauncher.launch("galaxy-memory-recovery.json") }.padding(horizontal = 12.dp, vertical = 9.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⌕", color = Cyan, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                decorationBox = { inner -> Box { if (search.isEmpty()) Text("Search saved memory...", color = Muted, fontSize = 11.sp); inner() } }
            )
            if (search.isNotEmpty()) Text("×", color = Muted, modifier = Modifier.clickable { search = "" }.padding(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            categories.forEach { category ->
                val active = selectedCategory == category
                Text(category, color = if (active) Color.White else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (active) Color(0xFF302A5B) else Panel).clickable { selectedCategory = category }.padding(horizontal = 11.dp, vertical = 10.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SAVED MEMORIES", color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("${filtered.size} item(s) • persistent local storage", color = Muted, fontSize = 10.sp)
            }
            Text("+ ADD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Violet).clickable {
                    editingMemoryId = null
                    noteDraft = ""
                    selectedCategory = "NOTES"
                    showEditor = true
                }.padding(horizontal = 14.dp, vertical = 11.dp))
        }
        if (showEditor) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel2).padding(13.dp)) {
                Text(if (editingMemoryId == null) "SAVE A MEMORY" else "EDIT MEMORY", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.drop(1).forEach { category ->
                        Text(category, color = if (selectedCategory == category) Cyan else Muted, fontSize = 10.sp,
                            modifier = Modifier.clickable { selectedCategory = category }.padding(vertical = 7.dp))
                    }
                }
                Text("IMPORTANCE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(vertical = 7.dp)) {
                    listOf(1 to "LOW", 2 to "NORMAL", 3 to "HIGH").forEach { (value, label) ->
                        Text(label, color = if (memoryPriority == value) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(if (memoryPriority == value) Color(0xFF302A5B) else Panel).clickable { memoryPriority = value }.padding(horizontal = 11.dp, vertical = 8.dp))
                    }
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = noteDraft, onValueChange = { noteDraft = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(vertical = 8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, lineHeight = 17.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                    decorationBox = { inner -> Box { if (noteDraft.isEmpty()) Text("কোনো তথ্য, নির্দেশনা, project update বা task লিখে রাখ...", color = Muted, fontSize = 10.sp); inner() } }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("CANCEL", color = Muted, modifier = Modifier.clickable {
                        showEditor = false; noteDraft = ""; editingMemoryId = null; selectedCategory = "ALL"
                    }.padding(9.dp))
                    Text(if (editingMemoryId == null) "SAVE MEMORY" else "UPDATE MEMORY", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        val body = noteDraft.trim()
                        val category = if (selectedCategory in categories.drop(1)) selectedCategory else "NOTES"
                        val editingId = editingMemoryId
                        if (body.isNotEmpty()) {
                            val duplicate = GalaxyMemoryStore.isDuplicate(context, category, body, editingId)
                            val saved = if (editingId == null) GalaxyMemoryStore.addOrRefresh(context, category, body, priority = memoryPriority)
                                else if (duplicate) false else GalaxyMemoryStore.update(context, editingId, category, body, priority = memoryPriority)
                            if (saved) {
                                notes = GalaxyMemoryStore.getAll(context)
                                noteDraft = ""; showEditor = false; editingMemoryId = null; selectedCategory = "ALL"
                                notice = if (editingId != null) "Memory updated successfully."
                                    else if (duplicate) "Existing memory refreshed instead of creating a duplicate."
                                    else "Memory saved on this device."
                            } else {
                                notice = if (duplicate) "এই category-তে একই memory আগে থেকেই আছে। Duplicate update করা হয়নি।"
                                    else "Memory save করা যায়নি। আবার চেষ্টা কর।"
                            }
                        } else {
                            notice = "Memory খালি রাখা যাবে না।"
                        }
                    }.padding(9.dp))
                }
            }
        }
        if (notice.isNotBlank()) Text(notice, color = Cyan, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.radialGradient(listOf(Color(0xFF25234D), Panel, Space))).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("◈", color = Violet, fontSize = 48.sp)
                Text(if (search.isEmpty()) "Your memory starts here" else "No matching memories", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("নিজের গুরুত্বপূর্ণ তথ্য ও project update এখানে যোগ কর।", color = Muted, fontSize = 10.sp)
            }
        } else {
            filtered.forEach { item ->
                Column(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.category, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("LOCAL", color = Muted, fontSize = 9.sp)
                        Text(" EDIT ", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                editingMemoryId = item.id
                                selectedCategory = item.category
                                noteDraft = item.content
                                memoryPriority = item.priority
                                showEditor = true
                            }.padding(start = 8.dp))
                        Text("  ×", color = Color(0xFFFFA7B8), fontSize = 16.sp,
                            modifier = Modifier.clickable {
                                val movedToTrash = GalaxyMemoryStore.delete(context, item.id)
                                notes = GalaxyMemoryStore.getAll(context)
                                trashItems = GalaxyMemoryStore.getTrash(context)
                                notice = if (movedToTrash) "Memory Trash-এ পাঠানো হয়েছে; Restore করা যাবে।" else "Memory delete করা যায়নি।"
                                if (editingMemoryId == item.id) {
                                    editingMemoryId = null; showEditor = false; noteDraft = ""
                                }
                            }.padding(start = 8.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("IMPORTANCE: " + when (item.priority) { 3 -> "HIGH"; 1 -> "LOW"; else -> "NORMAL" } + "  •  CONFIDENCE: ${(item.confidence * 100).toInt()}%", color = Muted, fontSize = 8.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(item.content, color = Color(0xFFE2E6F5), fontSize = 11.sp, lineHeight = 17.sp)
                    Text(java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.createdAt)), color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 7.dp))
                    Text("VERSION HISTORY (${GalaxyMemoryStore.versions(context, item.id).size})", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { historyMemoryId = if (historyMemoryId == item.id) null else item.id }.padding(top = 8.dp))
                    if (historyMemoryId == item.id) {
                        GalaxyMemoryStore.versions(context, item.id).forEach { version ->
                            Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(9.dp)).background(Panel2).padding(8.dp)) {
                                Text(java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(version.savedAt)), color = Muted, fontSize = 8.sp)
                                Text(version.entry.content, color = Color.White, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                                Text("RESTORE THIS VERSION", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        val restored = GalaxyMemoryStore.restoreVersion(context, item.id, version)
                                        notes = GalaxyMemoryStore.getAll(context)
                                        notice = if (restored) "Previous version restored. The replaced version is also kept in history." else "Could not restore: duplicate or missing memory."
                                    }.padding(top = 6.dp))
                            }
                        }
                        if (GalaxyMemoryStore.versions(context, item.id).isEmpty()) Text("Edit this memory to start its version history.", color = Muted, fontSize = 8.sp)
                    }
                }
            }
        }
        if (trashItems.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFF30232F)).padding(12.dp)) {
                Text("MEMORY TRASH • ${trashItems.size}", color = Color(0xFFFFC2A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("মুছে ফেলা memory এখানে থাকে। Restore করলে নতুন ID দিয়ে Vault-এ ফিরবে। সর্বোচ্চ ১০০টি item রাখা হয়।", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
                trashItems.forEach { deleted ->
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(9.dp)).background(Panel).padding(9.dp)) {
                        Text(deleted.entry.category, color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(deleted.entry.content, color = Color.White, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                        Text("RESTORE", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                val restored = GalaxyMemoryStore.restoreDeleted(context, deleted.entry.id)
                                notes = GalaxyMemoryStore.getAll(context)
                                trashItems = GalaxyMemoryStore.getTrash(context)
                                notice = if (restored) "Deleted memory restored to Vault." else "Restore failed: matching memory may already exist."
                            }.padding(top = 6.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SMART BACKUP CENTER", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("${snapshots.size}/5 local restore points", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(if (showBackupCenter) "HIDE" else "OPEN", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showBackupCenter = !showBackupCenter }.padding(8.dp))
            }
            Text("Snapshots এই ডিভাইসেই থাকে। Export করে আলাদা ফাইল রাখলে device হারালেও backup থাকবে। Restore নিরাপদ merge করে; বর্তমান memory overwrite বা delete করে না।", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 5.dp))
            Text("＋ CREATE RESTORE POINT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 9.dp).clip(RoundedCornerShape(9.dp)).background(Panel2).clickable {
                    val saved = GalaxyMemoryStore.createSnapshot(context)
                    snapshots = GalaxyMemoryStore.getSnapshots(context)
                    notice = if (saved) "Local restore point created." else "Could not save restore point. Check device storage."
                }.padding(horizontal = 12.dp, vertical = 10.dp))
            if (showBackupCenter) {
                if (snapshots.isEmpty()) Text("এখনো কোনো restore point নেই। Create Restore Point চাপ।", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
                snapshots.forEach { snapshot ->
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(9.dp)).background(Panel2).padding(9.dp)) {
                        Text(java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(snapshot.createdAt)), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${snapshot.memoryCount} memories • Local snapshot", color = Muted, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
                        Text("RESTORE (SAFE MERGE)", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                try {
                                    val result = GalaxyMemoryStore.restoreSnapshot(context, snapshot.id)
                                    notes = GalaxyMemoryStore.getAll(context)
                                    snapshots = GalaxyMemoryStore.getSnapshots(context)
                                    notice = "Snapshot restored by merge: ${result.imported} added, ${result.skippedDuplicates} duplicates skipped."
                                } catch (error: Exception) { notice = "Snapshot restore failed: ${error.message ?: "Unknown error"}" }
                            }.padding(top = 7.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("EXPORT FILE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel2).clickable {
                    exportLauncher.launch("Galaxy-Memory-Backup.json")
                }.padding(horizontal = 9.dp, vertical = 13.dp))
            Text("IMPORT FILE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel2).clickable {
                    importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                }.padding(horizontal = 9.dp, vertical = 13.dp))
            Text("CLEAR ALL", color = Color(0xFFFFA7B8), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Panel2).clickable {
                    GalaxyMemoryStore.clear(context); notes = emptyList(); trashItems = emptyList(); snapshots = emptyList(); historyMemoryId = null; notice = "All local memories, timeline, trash and restore points cleared."
                }.padding(horizontal = 9.dp, vertical = 13.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text("Local-only memory: এই build-এ Galaxy নিজে থেকে সব পুরোনো chat পড়তে পারে না। তুই এখানে যোগ করলে তা app বন্ধ করলেও থাকবে। কোনো cloud/Firebase sync নেই।", color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CyberLabScreen() {
    var activeSection by remember { mutableStateOf("OVERVIEW") }
    var selectedPath by remember { mutableStateOf("Cybersecurity Foundations") }
    var completed by remember { mutableStateOf(setOf<String>()) }
    var sandboxTarget by remember { mutableStateOf("Local practice environment") }
    var sandboxStatus by remember { mutableStateOf("IDLE") }
    var selectedTool by remember { mutableStateOf("Network Concepts") }
    val toolGuides = mapOf(
        "Network Concepts" to "An IP address identifies a host on a network. A port identifies a service endpoint. TCP provides an ordered, reliable byte stream; UDP sends independent datagrams with lower protocol overhead. Practice: explain why a web server commonly listens on port 443.",
        "Password Safety" to "Use a unique passphrase for every account and store it in a reputable password manager. Turn on multi-factor authentication, prefer phishing-resistant methods when available, and never share one-time codes. Practice: identify why reusing one password creates a chain-reaction risk.",
        "Log Explorer" to "Read logs by timestamp, source, event type and outcome. Establish a normal baseline before investigating anomalies. Repeated failed logins, unusual time windows and unexpected privilege changes are signals to review—not proof of compromise. Practice: group events by source and time.",
        "Hash Learning" to "A cryptographic hash maps input data to a fixed-length digest. Secure hashes are designed to make it impractical to recover the input or find collisions. Hashes are not encryption and cannot be decrypted. Password storage should use a password-hashing scheme such as Argon2id, scrypt or bcrypt with unique salts.",
        "HTTP Inspector" to "HTTP requests contain a method, path, headers and sometimes a body. Responses contain a status code, headers and an optional body. HTTPS protects HTTP traffic in transit using TLS. Practice: compare GET and POST and explain why status 401 differs from 403.",
        "Linux Command Guide" to "Start with safe navigation and inspection: pwd shows the current directory, ls lists files, cd changes directory, cat displays text and grep searches text. Understand paths and permissions before using commands that modify or delete files."
    )
    val paths = listOf(
        "Cybersecurity Foundations" to "Core concepts, ethics and safe security habits",
        "Networking Basics" to "IP, DNS, ports and network fundamentals",
        "Linux & Termux" to "Command line, files and system basics",
        "Web Security Basics" to "HTTP, sessions and common web risks",
        "Python for Security" to "Automation and defensive scripting"
    )
    val tools = listOf(
        "Network Concepts" to "Learn IP addresses, ports and protocols",
        "Password Safety" to "Understand passphrases and account protection",
        "Log Explorer" to "Practice reading sample security logs",
        "Hash Learning" to "Explore hashes with safe sample strings",
        "HTTP Inspector" to "Understand requests and response headers",
        "Linux Command Guide" to "Browse beginner command examples"
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DEFENSIVE SECURITY WORKSPACE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                Text("Cyber Lab", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("শেখো, অনুশীলন করো, নিরাপদ থাকো।", color = Muted, fontSize = 11.sp)
            }
            Box(Modifier.size(45.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("⌘", color = Cyan, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Color(0xFF1C3450), Panel))).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LEARNING PROGRESS", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("${completed.size} / ${paths.size} paths completed", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Progress is stored only while this screen remains active.", color = Muted, fontSize = 10.sp)
            }
            Box(Modifier.size(58.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("${(completed.size * 100 / paths.size)}%", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("OVERVIEW", "PATHS", "SANDBOX", "TOOLS").forEach { section ->
                val active = activeSection == section
                Text(section, color = if (active) Color.White else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (active) Color(0xFF302A5B) else Color.Transparent).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { activeSection = section }.padding(vertical = 11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(15.dp))
        when (activeSection) {
            "OVERVIEW" -> {
                Text("WELCOME TO THE LAB", color = Cyan, fontSize = 10.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text("Build skills the safe way.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("A guided workspace for defensive security, digital safety and practical fundamentals.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf("LEARNING PATHS" to "5 guided tracks", "SAFE SANDBOX" to "Local demo only").forEach { item ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Panel).padding(13.dp)) {
                            Text("✧", color = Violet, fontSize = 20.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(item.first, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(item.second, color = Muted, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("CONTINUE LEARNING", color = Cyan, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                paths.take(3).forEach { path ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { selectedPath = path.first; activeSection = "PATHS" }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(path.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(path.second, color = Muted, fontSize = 10.sp)
                        }
                        Text(if (completed.contains(path.first)) "DONE" else "OPEN  ›", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            "PATHS" -> {
                Text("GUIDED LEARNING PATHS", color = Cyan, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Choose a track and mark it complete when you finish.", color = Muted, fontSize = 10.sp)
                paths.forEach { path ->
                    val done = completed.contains(path.first)
                    Column(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(16.dp)).background(if (selectedPath == path.first) Color(0xFF222448) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { selectedPath = path.first }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (done) "✓" else "◈", color = if (done) Cyan else Violet, fontSize = 19.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(path.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(path.second, color = Muted, fontSize = 10.sp)
                            }
                            Text(if (done) "DONE" else if (selectedPath == path.first) "SELECTED" else "OPEN", color = if (done) Cyan else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (selectedPath == path.first) {
                            Spacer(Modifier.height(10.dp))
                            Text("Suggested lessons: Introduction • Key terms • Safe practice • Knowledge check", color = Color(0xFFD1D8EF), fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (done) "MARKED COMPLETE" else "MARK PATH COMPLETE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { completed = if (done) completed - path.first else completed + path.first }.padding(vertical = 6.dp))
                        }
                    }
                }
            }
            "SANDBOX" -> {
                Text("SAFE SANDBOX", color = Cyan, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Practice without touching real systems.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("This is a visual simulator only. It does not run commands, scan networks or connect to external targets.", color = Muted, fontSize = 10.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(13.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color(0xFF080D18)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LOCAL LAB CONSOLE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("● ${sandboxStatus}", color = if (sandboxStatus == "IDLE") Muted else Cyan, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("SELECT PRACTICE MODULE", color = Muted, fontSize = 10.sp)
                    listOf("Local practice environment", "Sample log analysis", "HTTP request walkthrough").forEach { target ->
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(10.dp)).background(if (sandboxTarget == target) Color(0xFF20294A) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { sandboxTarget = target; sandboxStatus = "IDLE" }.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(target, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                            Text(if (sandboxTarget == target) "●" else "○", color = Cyan, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("> galaxy-lab --preview", color = Color(0xFF91A6C7), fontSize = 10.sp)
                    Text("> module: ${sandboxTarget}", color = Color(0xFF91A6C7), fontSize = 10.sp)
                    Text("> environment: isolated demo", color = Color(0xFF91A6C7), fontSize = 10.sp)
                    Text("> status: ${sandboxStatus.lowercase()}", color = Cyan, fontSize = 10.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("RUN PREVIEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Violet).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { sandboxStatus = "PREVIEW" }.padding(horizontal = 13.dp, vertical = 10.dp))
                        Text("RESET", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { sandboxStatus = "IDLE" }.padding(horizontal = 13.dp, vertical = 10.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Safety boundary: no exploit payloads, credential attacks, unauthorized scanning or real target execution.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            "TOOLS" -> {
                Text("DEFENSIVE TOOLS DIRECTORY", color = Cyan, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Learning aids and safe sample-data utilities.", color = Muted, fontSize = 10.sp)
                tools.forEach { tool ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(if (selectedTool == tool.first) Color(0xFF25294F) else Panel).semantics { role = Role.Button }.clickable { selectedTool = tool.first }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(35.dp).clip(RoundedCornerShape(11.dp)).background(Panel2), contentAlignment = Alignment.Center) { Text("⌘", color = Cyan, fontSize = 17.sp) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tool.first, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(tool.second, color = Muted, fontSize = 10.sp)
                        }
                        Text(if (selectedTool == tool.first) "OPEN" else "GUIDE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(13.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF101A2B)).padding(14.dp)) {
                    Text("LEARNING GUIDE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(selectedTool, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(toolGuides[selectedTool] ?: "Choose a guide to begin.", color = Color(0xFFD5DDF1), fontSize = 11.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("SAFE PRACTICE • CONCEPTS ONLY", color = Color(0xFF8BE0B5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFF14283A)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 10.sp)
            Spacer(Modifier.width(8.dp))
            Text("UI preview only • no live cyber operations or external connections.", color = Color(0xFFB8D8E8), fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ResearchHubScreen() {
    var section by remember { mutableStateOf("OVERVIEW") }
    var selectedDataset by remember { mutableStateOf("Student Study Habits (sample)") }
    var importedFile by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("Study Time & GPA Analysis") }
    var projectCreated by remember { mutableStateOf(false) }
    var reportPreview by remember { mutableStateOf(false) }
    val datasets = listOf("Student Study Habits (sample)", "Campus Survey (sample)", "Health Indicators (sample)")
    val sampleRows = listOf(
        listOf("ID", "Study hrs", "Sleep hrs", "GPA"),
        listOf("S001", "3.5", "7.0", "3.60"),
        listOf("S002", "2.0", "6.0", "3.20"),
        listOf("S003", "5.0", "8.0", "3.85"),
        listOf("S004", "4.0", "7.5", "3.72")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DATA • STATISTICS • DISCOVERY", color = Cyan, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Text("Research Hub", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Explore data and shape your findings.", color = Muted, fontSize = 11.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("⌁", color = Cyan, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF1D3552), Panel))).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("RESEARCH WORKSPACE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("3 sample datasets", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("1 project draft • demo charts", color = Muted, fontSize = 10.sp)
            }
            Text("✦", color = Violet, fontSize = 32.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Panel).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("OVERVIEW", "DATASETS", "ANALYSIS", "PROJECTS", "REPORT").forEach { tab ->
                Text(tab, color = if (section == tab) Color.White else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (section == tab) Color(0xFF302A5B) else Color.Transparent).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { section = tab }.padding(vertical = 11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(15.dp))
        when (section) {
            "OVERVIEW" -> {
                Text("YOUR RESEARCH DESK", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("From raw data to clear insight.", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("A focused workspace for organizing datasets, planning analysis and preparing research reports.", color = Muted, fontSize = 10.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DATASETS" to "03", "PROJECTS" to "01", "REPORTS" to "01").forEach { item ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(Panel).padding(11.dp)) {
                            Text(item.second, color = Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(item.first, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text("RECENT DATASET", color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { section = "DATASETS" }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedDataset, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Sample data • 4 observations • 3 variables", color = Muted, fontSize = 10.sp)
                    }
                    Text("OPEN  ›", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(15.dp))
                Text("QUICK ANALYSIS", color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                listOf("Descriptive statistics", "Correlation overview", "Distribution chart").forEach { label ->
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(12.dp)).background(Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { section = "ANALYSIS" }.padding(12.dp)) {
                        Text("◈", color = Violet, fontSize = 15.sp)
                        Spacer(Modifier.width(9.dp))
                        Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text("VIEW", color = Muted, fontSize = 10.sp)
                    }
                }
            }
            "DATASETS" -> {
                Text("DATASET WORKSPACE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Manage your research data.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                datasets.forEach { dataset ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 7.dp).clip(RoundedCornerShape(12.dp)).background(if (selectedDataset == dataset) Color(0xFF25294F) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { selectedDataset = dataset }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("▦", color = Cyan, fontSize = 18.sp)
                        Spacer(Modifier.width(9.dp))
                        Text(dataset, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text(if (selectedDataset == dataset) "ACTIVE" else "SELECT", color = if (selectedDataset == dataset) Cyan else Muted, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(13.dp)) {
                    Text("IMPORT CSV FILE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("CSV import interface preview. File picker and parsing will be connected in a later data-engine phase.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(if (importedFile.isEmpty()) "＋  CHOOSE CSV FILE" else "✓  $importedFile", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Violet).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { importedFile = "sample_research_data.csv (demo)" }.padding(horizontal = 13.dp, vertical = 10.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("DATA PREVIEW • $selectedDataset", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(8.dp)) {
                    sampleRows.forEachIndexed { index, cells ->
                        Row(Modifier.fillMaxWidth().background(if (index == 0) Panel2 else Color.Transparent).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            cells.forEach { cell -> Text(cell, color = if (index == 0) Cyan else Color(0xFFDDE2F2), fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                        }
                    }
                }
            }
            "ANALYSIS" -> {
                Text("STATISTICAL ANALYSIS", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Explore the sample indicators.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Illustrative values only — calculations are not connected to a live dataset yet.", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mean study" to "3.63 h", "Mean GPA" to "3.59", "Mean sleep" to "7.13 h").forEach { item ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(Panel).padding(10.dp)) {
                            Text(item.first, color = Muted, fontSize = 10.sp)
                            Text(item.second, color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("STUDY HOURS BY SAMPLE STUDENT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Canvas(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)).background(Panel).padding(12.dp)) {
                    val values = listOf(3.5f, 2f, 5f, 4f)
                    val max = 6f
                    val gap = size.width / (values.size * 2 + 1)
                    values.forEachIndexed { i, v ->
                        val left = gap * (1 + i * 2)
                        val top = size.height - (v / max * size.height)
                        drawRoundRect(Color(0xFF55DFFF), Offset(left, top), androidx.compose.ui.geometry.Size(gap, size.height - top), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("DESCRIPTIVE SUMMARY", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                listOf("Study hours" to "Mean 3.63 • Min 2.0 • Max 5.0", "Sleep hours" to "Mean 7.13 • Min 6.0 • Max 8.0", "GPA" to "Mean 3.59 • Min 3.20 • Max 3.85").forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(11.dp)).background(Panel).padding(11.dp)) {
                        Text(row.first, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text(row.second, color = Muted, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Planned methods: frequency tables • mean/median • standard deviation • correlation • linear regression.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
            "PROJECTS" -> {
                Text("RESEARCH PROJECTS", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Organize your investigations.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(13.dp)) {
                    Text("NEW PROJECT NAME", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(projectName, color = Color.White, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Panel2).padding(12.dp))
                    Text("Tap below to cycle through suggested project titles.", color = Muted, fontSize = 10.sp)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("EDIT TITLE", color = Color.White, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { projectName = if (projectName.startsWith("Study")) "Campus Attendance & GPA" else "Study Time & GPA Analysis" }.padding(10.dp))
                        Text("CREATE PROJECT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Violet).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { projectCreated = true }.padding(10.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Panel).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (projectCreated) projectName else "Study Time & GPA Analysis", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(if (projectCreated) "Draft created in this screen session" else "Draft workspace • sample project", color = Muted, fontSize = 10.sp)
                    }
                    Text(if (projectCreated) "DRAFT" else "SAMPLE", color = Cyan, fontSize = 10.sp)
                }
                Text("Project state is temporary UI state; cloud/database saving is not active.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp))
            }
            "REPORT" -> {
                Text("REPORT PREVIEW", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Research summary draft.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F1F8)).padding(16.dp)) {
                    Text("RESEARCH REPORT", color = Color(0xFF252A48), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Study Time, Sleep Duration and GPA", color = Color(0xFF171A2C), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Sample analysis • Draft preview", color = Color(0xFF62677D), fontSize = 10.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("1. Objective", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Explore illustrative relationships between study habits and academic performance.", color = Color(0xFF44495D), fontSize = 10.sp, lineHeight = 14.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("2. Dataset", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Four fictional observations with study hours, sleep hours and GPA.", color = Color(0xFF44495D), fontSize = 10.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("3. Preliminary findings", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Descriptive values are shown for interface demonstration only. No inferential conclusion should be drawn.", color = Color(0xFF44495D), fontSize = 10.sp, lineHeight = 14.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("4. Limitations", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Synthetic sample; not suitable for academic claims or publication.", color = Color(0xFF44495D), fontSize = 10.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(if (reportPreview) "Preview refreshed • no file exported" else "Preview is a visual draft; PDF/export is not connected.", color = Muted, fontSize = 10.sp)
                Text("REFRESH PREVIEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(9.dp)).background(Violet).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { reportPreview = true }.padding(horizontal = 13.dp, vertical = 10.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF14283A)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 10.sp)
            Spacer(Modifier.width(8.dp))
            Text("UI preview only • no live data processing, file parsing or AI Core connection.", color = Color(0xFFB8D8E8), fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VisionStudioScreen() {
    var activeTab by remember { mutableStateOf("CAMERA") }
    var cameraState by remember { mutableStateOf("READY") }
    var selectedImage by remember { mutableStateOf("Galaxy sample • nebula.jpg") }
    var uploadState by remember { mutableStateOf("No file selected") }
    var analysisState by remember { mutableStateOf("IDLE") }
    var showError by remember { mutableStateOf(false) }
    var showGalleryEmpty by remember { mutableStateOf(false) }
    val gallery = listOf("Nebula sample.jpg", "Galaxy cluster.png", "Planet surface.jpg", "Star field.png")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CAMERA • IMAGES • VISUAL INTELLIGENCE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("Vision Studio", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Capture, organize and explore visual content.", color = Muted, fontSize = 10.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("◉", color = Cyan, fontSize = 23.sp, modifier = Modifier.semantics { contentDescription = "Vision Studio" })
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Panel).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("CAMERA", "UPLOAD", "GALLERY", "ANALYSIS").forEach { tab ->
                Text(tab, color = if (activeTab == tab) Color.White else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (activeTab == tab) Color(0xFF302A5B) else Color.Transparent).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { activeTab = tab }.padding(vertical = 11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(14.dp))
        when (activeTab) {
            "CAMERA" -> {
                Text("CAMERA PREVIEW", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(19.dp)).background(Brush.radialGradient(listOf(Color(0xFF30366B), Color(0xFF11162A), Color(0xFF070914)))), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        drawCircle(Color(0x3355DFFF), size.minDimension * 0.28f, Offset(cx, cy))
                        drawCircle(Cyan.copy(alpha = 0.45f), size.minDimension * 0.18f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                        drawCircle(Violet.copy(alpha = 0.5f), size.minDimension * 0.29f, Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                        val corners = 24f
                        val len = 27f
                        listOf(Offset(20f,20f), Offset(size.width-20f,20f), Offset(20f,size.height-20f), Offset(size.width-20f,size.height-20f)).forEachIndexed { i,p ->
                            val sx = if (i % 2 == 0) 1f else -1f
                            val sy = if (i < 2) 1f else -1f
                            drawLine(Cyan.copy(alpha=0.7f), p, Offset(p.x + sx*len,p.y), 2f)
                            drawLine(Cyan.copy(alpha=0.7f), p, Offset(p.x,p.y + sy*len), 2f)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("◉", color = Cyan, fontSize = 42.sp, modifier = Modifier.semantics { contentDescription = "Camera preview placeholder" })
                        Spacer(Modifier.height(8.dp))
                        Text("CAMERA PREVIEW PLACEHOLDER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Camera permission and hardware preview are not connected.", color = Muted, fontSize = 10.sp)
                    }
                    Text("● ${cameraState}", color = if (cameraState == "READY") Cyan else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(13.dp))
                    Text("16:9  •  PREVIEW", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(13.dp))
                }
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("START PREVIEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Violet).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { cameraState = "DEMO" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("CAPTURE DEMO", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { selectedImage = "New demo capture • preview.png"; uploadState = "Demo capture selected"; activeTab = "ANALYSIS" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                Text("CAMERA STATES", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                    listOf("READY", "LOADING", "ERROR").forEach { state ->
                        Text(state, color = if (cameraState == state) Color.White else Muted, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (cameraState == state) Color(0xFF302A5B) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { cameraState = state }.padding(horizontal = 12.dp, vertical = 9.dp))
                    }
                }
                if (cameraState == "ERROR") VisionStateCard("Camera unavailable", "Camera access is not configured in this build. Check permission and device support after integration.", "ERROR")
                if (cameraState == "LOADING") VisionStateCard("Preparing preview…", "This is a loading-state design preview. No camera service is starting.", "LOADING")
            }
            "UPLOAD" -> {
                Text("IMAGE UPLOAD", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Bring an image into your workspace.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF252A55), Color(0xFF101426)))), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⇧", color = Cyan, fontSize = 38.sp)
                            Text("SELECT IMAGE FILE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("JPG • PNG • WEBP", color = Muted, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("＋  CHOOSE IMAGE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Violet).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { uploadState = "Demo image selected"; selectedImage = "Uploaded sample • image.png"; showError = false }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(uploadState, color = if (uploadState.startsWith("No")) Muted else Cyan, fontSize = 10.sp)
                    Text("File picker and real image decoding will be connected in a later integration phase.", color = Muted, fontSize = 10.sp, lineHeight = 13.sp)
                }
                Spacer(Modifier.height(11.dp))
                Text("UPLOAD VALIDATION STATES", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 7.dp)) {
                    Text("SIMULATE ERROR", color = Color.White, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { showError = true }.padding(10.dp))
                    Text("RESET", color = Muted, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { showError = false; uploadState = "No file selected" }.padding(10.dp))
                }
                if (showError) VisionStateCard("Unsupported file", "This demo state represents an invalid format or file that could not be read.", "ERROR")
            }
            "GALLERY" -> {
                Text("IMAGE GALLERY & HISTORY", color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("Your recent visual workspace.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SAMPLE ITEMS", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f).padding(top = 9.dp))
                    Text(if (showGalleryEmpty) "SHOW ITEMS" else "EMPTY STATE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { showGalleryEmpty = !showGalleryEmpty }.padding(8.dp))
                }
                if (showGalleryEmpty) {
                    VisionStateCard("No images yet", "Your uploaded images and captures will appear here once the gallery storage is connected.", "EMPTY")
                } else {
                    gallery.chunked(2).forEachIndexed { rowIndex, rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(bottom = 9.dp)) {
                            rowItems.forEachIndexed { colIndex, item ->
                                val active = selectedImage == item
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (active) Color(0xFF25294F) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { selectedImage = item; activeTab = "ANALYSIS" }.padding(9.dp)) {
                                    Box(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(10.dp)).background(Brush.radialGradient(listOf(if ((rowIndex+colIndex)%2==0) Color(0xFF48518C) else Color(0xFF24546C), Color(0xFF101426)))), contentAlignment = Alignment.Center) {
                                        Text(if ((rowIndex+colIndex)%2==0) "✦" else "◉", color = Cyan, fontSize = 31.sp)
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(item, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Sample • preview only", color = Muted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                Text("Gallery items are illustrative placeholders; no image files are stored.", color = Muted, fontSize = 10.sp)
            }
            "ANALYSIS" -> {
                Text("ANALYSIS RESULT PREVIEW", color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("Visual insight workspace.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(13.dp)) {
                    Text("SELECTED IMAGE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(selectedImage, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Brush.radialGradient(listOf(Color(0xFF4A4F89), Color(0xFF15203B), Color(0xFF080B17)))), contentAlignment = Alignment.Center) {
                        Text("✦", color = Cyan, fontSize = 50.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("DEMO RESULT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Sample cosmic image • bright central region • scattered light points", color = Color(0xFFD7DDF0), fontSize = 10.sp, lineHeight = 15.sp)
                    Text("Confidence: not calculated • Model: not connected", color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RUN DEMO ANALYSIS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Violet).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { analysisState = "LOADING" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("SIMULATE ERROR", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { analysisState = "ERROR" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                if (analysisState == "LOADING") VisionStateCard("Analysis queued (demo)", "Loading state preview only. No image processing or AI request has been executed.", "LOADING")
                if (analysisState == "ERROR") VisionStateCard("Analysis unavailable", "No vision provider is connected. Try again after the analysis service is configured.", "ERROR")
                if (analysisState == "IDLE") VisionStateCard("Ready for preview", "Choose a sample image and preview the result layout.", "EMPTY")
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF14283A)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 10.sp)
            Spacer(Modifier.width(8.dp))
            Text("UI preview only • camera, file picker, gallery storage and vision AI are not connected.", color = Color(0xFFB8D8E8), fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VisionStateCard(title: String, detail: String, state: String) {
    val tint = if (state == "ERROR") Color(0xFFFF8B9B) else if (state == "LOADING") Cyan else Muted
    Row(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(13.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.Top) {
        Text(if (state == "ERROR") "!" else if (state == "LOADING") "◌" else "○", color = tint, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun NotificationCenterScreen() {
    data class Notice(val id:Int,val type:String,val title:String,val body:String,val time:String,val unread:Boolean)
    val seed=listOf(Notice(1,"SYSTEM","Galaxy UI updated","New workspace screens are ready to explore.","Just now",true),Notice(2,"RESEARCH","Research Hub ready","Dataset workspace and report preview are available.","12 min ago",true),Notice(3,"SECURITY","Learning reminder","Continue your defensive security learning path.","1 hour ago",true),Notice(4,"VISION","Vision Studio preview","Camera and image analysis UI is available.","Yesterday",false),Notice(5,"SYSTEM","Welcome to Galaxy","Your cosmic workspace is ready.","2 days ago",false))
    var items by remember { mutableStateOf(seed) }
    var filter by remember { mutableStateOf("ALL") }
    var selected by remember { mutableStateOf<Int?>(null) }
    var settings by remember { mutableStateOf(false) }
    var emptyDemo by remember { mutableStateOf(false) }
    var system by remember { mutableStateOf(true) }
    var research by remember { mutableStateOf(true) }
    var security by remember { mutableStateOf(true) }
    var vision by remember { mutableStateOf(true) }
    var push by remember { mutableStateOf(false) }
    val unread=items.count { it.unread }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp, vertical=10.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GALAXY ACTIVITY • UPDATES",color=Cyan,fontSize = 10.sp,letterSpacing=1.5.sp,fontWeight=FontWeight.Bold)
                Text("Notification Center",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)
                Text("Your workspace updates, organized.",color=Muted,fontSize=10.sp)
            }
            Box(Modifier.size(46.dp).clip(CircleShape).background(Panel2),contentAlignment=Alignment.Center) {
                Text("♢",color=Cyan,fontSize=25.sp)
                if(unread>0) Box(Modifier.align(Alignment.TopEnd).size(9.dp).clip(CircleShape).background(Color(0xFFFF718C)))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Color(0xFF203858),Panel))).padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("INBOX OVERVIEW",color=Cyan,fontSize = 10.sp,letterSpacing=1.3.sp,fontWeight=FontWeight.Bold)
                Text("$unread unread notifications",color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("${items.size} total • local demo inbox",color=Muted,fontSize = 10.sp)
            }
            Text("$unread",color=Cyan,fontSize=25.sp,fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(if(settings) "← INBOX" else "⚙ SETTINGS",color=Color.White,fontSize = 10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(Panel2).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { settings=!settings;selected=null }.padding(10.dp))
            Text("MARK ALL READ",color=Cyan,fontSize = 10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { items=items.map { it.copy(unread=false) } }.padding(10.dp))
            Text("CLEAR ALL",color=Color(0xFFFF9AA9),fontSize = 10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { items=emptyList();selected=null }.padding(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        if(settings) {
            Text("NOTIFICATION PREFERENCES",color=Cyan,fontSize = 10.sp,fontWeight=FontWeight.Bold)
            listOf("System updates" to system,"Research updates" to research,"Security reminders" to security,"Vision updates" to vision,"Push notifications" to push).forEachIndexed { i,p ->
                Row(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(12.dp)).background(Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { when(i){0->system=!system;1->research=!research;2->security=!security;3->vision=!vision;4->push=!push} }.padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(p.first,color=Color.White,fontSize=10.sp,modifier=Modifier.weight(1f))
                    Text(if(p.second) "ON" else "OFF",color=if(p.second) Cyan else Muted,fontSize = 10.sp,fontWeight=FontWeight.Bold)
                }
            }
            Text("Preferences are local UI state. Push delivery and Android notification permissions are not connected.",color=Muted,fontSize = 10.sp,lineHeight=14.sp,modifier=Modifier.padding(top=10.dp))
        } else if(selected!=null) {
            val n=items.firstOrNull { it.id==selected }
            if(n!=null) {
                Text("NOTIFICATION DETAIL",color=Cyan,fontSize = 10.sp,fontWeight=FontWeight.Bold)
                Column(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(15.dp)) {
                    Text(n.type,color=Cyan,fontSize = 10.sp,fontWeight=FontWeight.Bold)
                    Text(n.title,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=7.dp))
                    Text(n.time,color=Muted,fontSize = 10.sp)
                    Text(n.body,color=Color(0xFFD5DCEF),fontSize=11.sp,lineHeight=18.sp,modifier=Modifier.padding(top=14.dp))
                    Text(if(n.unread) "MARK AS READ" else "MARK AS UNREAD",color=Cyan,fontSize = 10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { items=items.map { if(it.id==n.id) it.copy(unread=!it.unread) else it } }.padding(top=16.dp))
                    Text("DELETE NOTIFICATION",color=Color(0xFFFF9AA9),fontSize = 10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { items=items.filterNot { it.id==n.id };selected=null }.padding(top=12.dp))
                }
            } else selected=null
        } else {
            Text("FILTER BY CATEGORY",color=Cyan,fontSize = 10.sp,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=7.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("ALL","SYSTEM","RESEARCH","SECURITY","VISION").forEach { f ->
                    Text(f,color=if(filter==f) Color.White else Muted,fontSize = 10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(if(filter==f) Color(0xFF302A5B) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { filter=f }.padding(horizontal=12.dp,vertical=10.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(if(filter=="ALL") "RECENT ACTIVITY" else "$filter NOTIFICATIONS",color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text(if(emptyDemo) "SHOW ITEMS" else "EMPTY PREVIEW",color=Muted,fontSize = 10.sp,modifier=Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { emptyDemo=!emptyDemo }.padding(6.dp))
            }
            val visible=if(emptyDemo) emptyList() else items.filter { filter=="ALL" || it.type==filter }
            if(visible.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top=10.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("◇",color=Violet,fontSize=34.sp)
                    Text("You're all caught up",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold)
                    Text("No notifications to show in this view.",color=Muted,fontSize = 10.sp)
                    Text("CLEAR FILTERS",color=Cyan,fontSize = 10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { filter="ALL";emptyDemo=false }.padding(top=10.dp))
                }
            } else visible.forEach { n ->
                Row(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(14.dp)).background(if(n.unread) Color(0xFF202744) else Panel).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable { selected=n.id;items=items.map { if(it.id==n.id) it.copy(unread=false) else it } }.padding(12.dp),verticalAlignment=Alignment.Top) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Panel2),contentAlignment=Alignment.Center) { Text(when(n.type){"RESEARCH"->"⌁";"SECURITY"->"◇";"VISION"->"◉";else->"✦"},color=Cyan,fontSize=18.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text(n.title,color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            if(n.unread) Box(Modifier.size(7.dp).clip(CircleShape).background(Cyan))
                        }
                        Text(n.body,color=Muted,fontSize = 10.sp,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=14.sp)
                        Text("${n.type}  •  ${n.time}",color=Color(0xFF8291B2),fontSize = 10.sp,modifier=Modifier.padding(top=5.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF14283A)).padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("●",color=Cyan,fontSize = 10.sp);Spacer(Modifier.width(8.dp))
            Text("Local UI demo • sample notifications, no live delivery service.",color=Color(0xFFB8D8E8),fontSize = 10.sp,lineHeight=14.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FeatureScreen(tag: String, title: String, subtitle: String, detail: String, action: String, status: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.Start) {
        Spacer(Modifier.height(18.dp))
        Text(tag, color = Cyan, fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(24.dp)).background(Brush.radialGradient(listOf(Color(0xFF34306C), Panel, Space))), contentAlignment = Alignment.Center) {
            Text("✦", color = Violet, fontSize = 74.sp)
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(18.dp)) {
            Text("MODULE OVERVIEW", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(detail, color = Color(0xFFD3D8EC), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(18.dp))
            Text(action.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(status, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProfileScreen(onSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("galaxy_profile", android.content.Context.MODE_PRIVATE) }
    var displayName by remember { mutableStateOf(prefs.getString("display_name", "Galaxy Explorer") ?: "Galaxy Explorer") }
    var draftName by remember { mutableStateOf(displayName) }
    var saved by remember { mutableStateOf(false) }
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "G"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("YOUR SPACE", color = Cyan, fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Explorer Profile", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Color(0xFF163E67)))), contentAlignment = Alignment.Center) {
                Text(initial, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            Text(displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Personal Galaxy workspace", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("PERSONALIZE PROFILE", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
            Text("DISPLAY NAME", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = draftName,
                onValueChange = { draftName = it.take(32); saved = false },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Panel2).padding(13.dp),
                decorationBox = { inner -> if (draftName.isEmpty()) Text("Enter a display name", color = Muted, fontSize = 13.sp); inner() }
            )
            Spacer(Modifier.height(10.dp))
            Text("SAVE PROFILE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Violet).heightIn(min = 44.dp).clickable {
                    val clean = draftName.trim()
                    if (clean.isNotEmpty()) {
                        displayName = clean
                        prefs.edit().putString("display_name", clean).apply()
                        saved = true
                    }
                }.padding(horizontal = 14.dp, vertical = 12.dp))
            if (saved) Text("Profile saved on this device.", color = Color(0xFF8BE0B5), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("APP INFORMATION", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        StatusPanel()
        Spacer(Modifier.height(16.dp))
        Text("Version 0.1.0  •  Project Galaxy AI", color = Muted, fontSize = 10.sp)
        Spacer(Modifier.height(18.dp))
        ModuleCard("⚙", "APP SETTINGS", "Preferences and appearance", Modifier.fillMaxWidth(), onSettings)
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("galaxy_preferences", android.content.Context.MODE_PRIVATE) }
    var visualEffects by remember { mutableStateOf(prefs.getBoolean("visual_effects", true)) }
    var webPreference by remember { mutableStateOf(prefs.getBoolean("web_preference", false)) }
    var language by remember { mutableStateOf(prefs.getString("language", "বাংলা + English") ?: "বাংলা + English") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("CONTROL CENTER", color = Cyan, fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Shape your Galaxy experience.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))

        SettingsSectionTitle("APPEARANCE")
        SettingsToggleRow("Visual effects", "Enable enhanced visual effects", visualEffects) {
            visualEffects = it
            prefs.edit().putBoolean("visual_effects", it).apply()
        }
        SettingsInfoRow("Theme", "Cosmic dark • fixed for this build")
        SettingsSectionTitle("LANGUAGE")
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
            Text("App language preference", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            listOf("বাংলা + English", "বাংলা", "English").forEach { option ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).heightIn(min = 48.dp).semantics { role = Role.Button }.clickable {
                    language = option
                    prefs.edit().putString("language", option).apply()
                }.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(option, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(if (language == option) "● SELECTED" else "○", color = if (language == option) Cyan else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Preference saved on this device. Full interface localization will be applied in a later pass.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
        }

        Spacer(Modifier.height(18.dp))
        SettingsSectionTitle("OFFLINE DATA CENTER")
        OfflineDataPanel()
        Spacer(Modifier.height(18.dp))
        SettingsSectionTitle("SMART PERMISSION MANAGER")
        PermissionManagerPanel()
        Spacer(Modifier.height(16.dp))
        SettingsSectionTitle("ANDROID SETTINGS SWITCH CONTROL")
        SettingsSwitchControlPanel()
        SettingsInfoRow("Memory controls", "Memory service not connected")
        SettingsInfoRow("Data & privacy", "Local preferences stay on this device")
        Spacer(Modifier.height(18.dp))
        SettingsSectionTitle("AI PREFERENCES")
        SettingsToggleRow("Web access preference", "Saved preference only; web tools are not connected", webPreference) {
            webPreference = it
            prefs.edit().putBoolean("web_preference", it).apply()
        }
        SettingsInfoRow("Preferred model", "Choose from AI Models • demo only")
        Spacer(Modifier.height(12.dp))
        SettingsSectionTitle("PERSONALIZATION INTELLIGENCE")
        PersonalizationSettingsPanel()
        SettingsSectionTitle("API KEY MANAGER & SECURE VAULT")
        GalaxySecureVaultSettingsPanel()
        SettingsSectionTitle("VOICE PREFERENCES")
        var voiceGender by remember { mutableStateOf(prefs.getString("voice_gender", "Male") ?: "Male") }
        var settingsTtsReady by remember { mutableStateOf(false) }
        var bengaliVoiceCount by remember { mutableIntStateOf(0) }
        var voicePreviewStatus by remember { mutableStateOf("Voice engine যাচাই হচ্ছে…") }
        val settingsTts = remember { mutableStateOf<TextToSpeech?>(null) }

        DisposableEffect(context) {
            val engine = TextToSpeech(context) { result ->
                settingsTtsReady = result == TextToSpeech.SUCCESS
                if (result == TextToSpeech.SUCCESS) {
                    val voices = settingsTts.value?.voices ?: emptySet<Voice>()
                    bengaliVoiceCount = voices.count { it.locale.language.equals("bn", ignoreCase = true) }
                    voicePreviewStatus = if (bengaliVoiceCount > 0) {
                        "${bengaliVoiceCount}টি বাংলা voice পাওয়া গেছে।"
                    } else {
                        "বাংলা voice পাওয়া যায়নি; preview-তে device default fallback হবে।"
                    }
                } else {
                    voicePreviewStatus = "Text-to-Speech engine চালু হয়নি। Android Settings থেকে TTS engine পরীক্ষা করো।"
                }
            }
            settingsTts.value = engine
            onDispose {
                engine.stop()
                engine.shutdown()
                settingsTts.value = null
            }
        }

        fun previewSelectedVoice() {
            val engine = settingsTts.value
            if (!settingsTtsReady || engine == null) {
                voicePreviewStatus = "Voice engine এখনো প্রস্তুত নয়। একটু পরে আবার চেষ্টা করো।"
                return
            }
            val voices = engine.voices ?: emptySet<Voice>()
            val genderToken = if (voiceGender == "Female") "female" else "male"
            val matchingGender = voices.filter { it.name.contains(genderToken, ignoreCase = true) }
            val genderBengali = matchingGender.firstOrNull {
                it.locale.language.equals("bn", ignoreCase = true) &&
                    it.locale.country.equals("BD", ignoreCase = true)
            } ?: matchingGender.firstOrNull {
                it.locale.language.equals("bn", ignoreCase = true) &&
                    it.locale.country.equals("IN", ignoreCase = true)
            } ?: matchingGender.firstOrNull {
                it.locale.language.equals("bn", ignoreCase = true)
            }
            val anyBengali = voices.firstOrNull {
                it.locale.language.equals("bn", ignoreCase = true) &&
                    it.locale.country.equals("BD", ignoreCase = true)
            } ?: voices.firstOrNull {
                it.locale.language.equals("bn", ignoreCase = true)
            }
            val chosenVoice = genderBengali ?: anyBengali
            if (chosenVoice != null) {
                engine.voice = chosenVoice
                voicePreviewStatus = "Preview: ${chosenVoice.name} (${chosenVoice.locale.displayName})"
            } else {
                engine.setLanguage(Locale("bn", "BD"))
                voicePreviewStatus = "নির্দিষ্ট gender বাংলা voice পাওয়া যায়নি; default Bengali TTS ব্যবহার হচ্ছে।"
            }
            engine.setSpeechRate(0.95f)
            engine.speak(
                "আমি Galaxy। তোমার নির্বাচিত " + (if (voiceGender == "Female") "নারী" else "পুরুষ") + " কণ্ঠ পরীক্ষা করছি।",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "galaxy-settings-voice-preview"
            )
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
            Text("Galaxy speech voice", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Male", "Female").forEach { option ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (voiceGender == option) Color(0xFF29264F) else Panel2)
                        .semantics { role = Role.Button }.clickable {
                            voiceGender = option
                            prefs.edit().putString("voice_gender", option).apply()
                            voicePreviewStatus = "$option voice preference save হয়েছে। Preview চাপ দিয়ে পরীক্ষা করো।"
                        }.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (option == "Male") "♂ Male" else "♀ Female", color = if (voiceGender == option) Cyan else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Panel2)
                .semantics { role = Role.Button }.clickable { previewSelectedVoice() }
                .padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("▶  TEST SELECTED VOICE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(voicePreviewStatus, color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 8.dp))
            Text("বাংলা voice availability Android-এর installed TTS engine-এর ওপর নির্ভর করে। Gender tag না থাকলে app নিশ্চিতভাবে কণ্ঠের gender শনাক্ত করতে পারে না; তখন Bengali default voice fallback হবে.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 5.dp))
        }
        Spacer(Modifier.height(18.dp))
        SettingsSectionTitle("BLACK HOLE SETTINGS")
        var bhZoom by remember { mutableFloatStateOf(prefs.getFloat("bh_zoom", 1.0f)) }
        var bhSpeed by remember { mutableFloatStateOf(prefs.getFloat("bh_speed", 1.0f)) }
        var bhGlow by remember { mutableFloatStateOf(prefs.getFloat("bh_intensity", 1.0f)) }
        var bhParticles by remember { mutableFloatStateOf(prefs.getFloat("bh_particles", 0.65f)) }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
            Text("Zoom  ${String.format("%.1f", bhZoom)}×", color = Color.White, fontSize = 11.sp)
            Slider(value = bhZoom, onValueChange = { bhZoom = it; prefs.edit().putFloat("bh_zoom", it).apply() }, valueRange = 0.7f..1.8f)
            Text("Animation speed  ${String.format("%.1f", bhSpeed)}×", color = Color.White, fontSize = 11.sp)
            Slider(value = bhSpeed, onValueChange = { bhSpeed = it; prefs.edit().putFloat("bh_speed", it).apply() }, valueRange = 0.2f..2.5f)
            Text("Glow intensity  ${String.format("%.1f", bhGlow)}×", color = Color.White, fontSize = 11.sp)
            Slider(value = bhGlow, onValueChange = { bhGlow = it; prefs.edit().putFloat("bh_intensity", it).apply() }, valueRange = 0.3f..2.0f)
            Text("Particle density  ${String.format("%.0f", bhParticles * 100)}%", color = Color.White, fontSize = 11.sp)
            Slider(value = bhParticles, onValueChange = { bhParticles = it; prefs.edit().putFloat("bh_particles", it).apply() }, valueRange = 0.1f..1.0f)
            Text("Settings এই ডিভাইসে save হবে এবং Black Hole খুললে প্রয়োগ হবে.", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(18.dp))
        SettingsSectionTitle("ABOUT")
        SettingsInfoRow("App version", "0.1.0")
        SettingsInfoRow("Developer", "NATIONVK")
        SettingsInfoRow("Project", "Galaxy AI")
        Spacer(Modifier.height(12.dp))
        Text("Settings marked as preferences do not activate unavailable services.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}


@Composable
private fun OfflineDataPanel() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val memoryCount = remember(refresh) { GalaxyMemoryStore.getAll(context).size }
    val chatCount = remember(refresh) { GalaxyLocalConversationStore.load(context).size }
    val voiceCount = remember(refresh) { GalaxyVoiceHistory.getEntries(context).size }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DEVICE-ONLY STORAGE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text("তোর ডেটা এই ফোনেই থাকে; Cloud sync এখনো চালু নয়।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Text("REFRESH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).clickable { refresh++ }.padding(horizontal = 10.dp, vertical = 9.dp))
        }
        Spacer(Modifier.height(12.dp))
        StatusRow("Saved memories", memoryCount.toString(), Cyan)
        StatusRow("Offline chat messages", chatCount.toString(), Violet)
        StatusRow("Voice history records", voiceCount.toString(), Color(0xFFFFC66D))
        Spacer(Modifier.height(8.dp))
        Text("OFFLINE-READY", color = Color(0xFF8BE0B5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Memory Vault, saved chat notes, voice history, app preferences এবং Black Hole visual localভাবে ব্যবহার করা যায়। AI উত্তর, Cloud backup ও online research-এর জন্য Internet/পরবর্তী integration লাগবে।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text("পরামর্শ: গুরুত্বপূর্ণ ডেটার JSON backup Memory Vault থেকে Export করে রাখ। অ্যাপ uninstall বা device reset হলে local data হারাতে পারে।", color = Color(0xFFFFDCA8), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun VoiceCommandHistoryPanel() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(GalaxyVoiceHistory.getEntries(context)) }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("VOICE COMMAND HISTORY", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text("এই ডিভাইসে সর্বশেষ ৩০টি command", color = Muted, fontSize = 10.sp)
            }
            Text("REFRESH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).clickable {
                    entries = GalaxyVoiceHistory.getEntries(context)
                }.padding(horizontal = 10.dp, vertical = 8.dp))
            Spacer(Modifier.width(6.dp))
            Text("CLEAR", color = Color(0xFFFFA7B8), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).clickable {
                    GalaxyVoiceHistory.clear(context)
                    entries = emptyList()
                }.padding(horizontal = 10.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        if (entries.isEmpty()) {
            Text("এখনো কোনো background voice command সংরক্ষিত হয়নি।", color = Muted, fontSize = 11.sp)
        } else {
            entries.forEach { entry ->
                Text(entry, color = Color.White, fontSize = 10.sp, lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp))
            }
        }
        Text("History শুধু local storage-এ থাকে; কোনো cloud বা Firebase-এ পাঠানো হয় না।", color = Muted, fontSize = 9.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun PermissionManagerPanel() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val micGranted = refresh >= 0 && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val notificationDeclared = android.os.Build.VERSION.SDK_INT >= 33
    val notificationGranted = !notificationDeclared || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val accessibilityComponent = android.content.ComponentName(context, GalaxyAccessibilityService::class.java).flattenToString()
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    val accessibilityEnabled = refresh >= 0 && enabledServices.split(':').any { it.equals(accessibilityComponent, ignoreCase = true) }
    val accessibilityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Text("Permission Status", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Galaxy প্রয়োজনের সময় permission চাইবে। Allow না করলে সংশ্লিষ্ট কাজ চালু হবে না।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
        PermissionStatusRow("Microphone", "Voice Assistant ও Hey Galaxy", micGranted, if (micGranted) null else { { micLauncher.launch(Manifest.permission.RECORD_AUDIO) } })
        Spacer(Modifier.height(8.dp))
        PermissionStatusRow("Notifications", "Background listening-এর স্থায়ী status", notificationGranted, if (notificationGranted || !notificationDeclared) null else { { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } })
        Spacer(Modifier.height(8.dp))
        PermissionStatusRow("Accessibility • Settings Switch", "তোর নির্দেশে Settings action-এর জন্য optional access", accessibilityEnabled, if (accessibilityEnabled) null else { { accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        Spacer(Modifier.height(5.dp))
        Text("Android Accessibility Settings-এ গিয়ে Galaxy Settings Switch Access নিজে ON করবি। এই আপডেটে service foundation যোগ হয়েছে; এখনো switch click করে না। পরের আপডেটে allowlisted switch control যোগ হবে।", color = Color(0xFFB8D8E8), fontSize = 10.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
        PermissionStatusRow("Camera", "Vision feature ব্যবহার করলে চাইবে", false, null, "এখন প্রয়োজন নেই")
        Spacer(Modifier.height(8.dp))
        PermissionStatusRow("Contacts / Location", "নির্দিষ্ট feature চালু হলে চাইবে", false, null, "এখন প্রয়োজন নেই")
        Spacer(Modifier.height(10.dp))
        Text("Accessibility একটি শক্তিশালী special access। Galaxy এটি নিজে enable করতে পারে না; নিয়ন্ত্রণ তোর হাতেই থাকবে।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}


@Composable
private fun SettingsSwitchControlPanel() {
    val context = LocalContext.current
    var switchReport by remember { mutableStateOf("এখনো কোনো supported switch state শনাক্ত হয়নি।") }

    fun refreshSwitchReport() {
        val (snapshot, capturedAt) = GalaxyAccessibilityService.getLatestSwitchSnapshot()
        switchReport = if (snapshot.isEmpty()) {
            "কোনো switch state পাওয়া যায়নি। Android Settings-এর সংশ্লিষ্ট screen খুলে আবার ফিরে এসে Refresh কর।"
        } else {
            snapshot.entries.joinToString(" • ") { (name, enabled) -> "$name: ${if (enabled) "ON" else "OFF"}" } +
                if (capturedAt > 0L) "\\nশুধু Android Settings থেকে শনাক্ত; এটি live control নয়।" else ""
        }
    }

    fun openSystemSettings(action: String, extraKey: String? = null, extraValue: String? = null) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (extraKey != null && extraValue != null) intent.putExtra(extraKey, extraValue)
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Text("Supported system controls", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("DETECTED SWITCH STATES", color = Color(0xFF9D8CFF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Text(switchReport, color = Color(0xFFB8D8E8), fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
        Text("REFRESH STATUS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp, bottom = 8.dp).clip(RoundedCornerShape(8.dp)).background(Violet).clickable { refreshSwitchReport() }.padding(horizontal = 12.dp, vertical = 8.dp))
        Text("Android-এর নিরাপত্তা নিয়ম মেনে Galaxy প্রয়োজনীয় system panel বা Settings page খুলবে। যেখানে third-party app সরাসরি switch করতে পারে না, সেখানে তুই নিজে পরিবর্তন করবি।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
        SettingsActionRow("Wi-Fi", "System Wi-Fi panel খুলবে", "OPEN") {
            openSystemSettings("android.settings.panel.action.WIFI")
        }
        SettingsActionRow("Bluetooth", "Bluetooth settings খুলবে", "OPEN") {
            openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        SettingsActionRow("Location", "Location settings খুলবে", "OPEN") {
            openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        }
        SettingsActionRow("Display / Dark mode", "Display settings খুলবে", "OPEN") {
            openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
        }
        SettingsActionRow("Do Not Disturb", "DND access page; grant করা থাকলেও user confirmation ছাড়া বদলাবে না", "ACCESS") {
            openSystemSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
        SettingsActionRow("Airplane mode", "System page only; Galaxy সরাসরি toggle করতে পারবে না", "OPEN") {
            openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        }
        SettingsActionRow("App notifications", "এই Galaxy app-এর notification settings", "OPEN") {
            openSystemSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS, Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        Spacer(Modifier.height(8.dp))
        Text("এখনকার ধাপে এগুলো নিরাপদ Settings shortcuts। Accessibility দিয়ে অন্য app-এর UI-তে স্বয়ংক্রিয় click এখনো চালু করা হয়নি। Wi-Fi, Bluetooth ও Airplane Mode-এর direct toggle Android version/OEM policy দ্বারা সীমাবদ্ধ।", color = Color(0xFFB8D8E8), fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun SettingsActionRow(title: String, description: String, actionLabel: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 7.dp).clip(RoundedCornerShape(12.dp)).background(Panel2).padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 9.sp, lineHeight = 13.sp)
        }
        Text(actionLabel, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Violet).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp))
    }
}

@Composable
private fun PermissionStatusRow(title: String, description: String, granted: Boolean, onRequest: (() -> Unit)?, deferred: String? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel2).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 9.sp, lineHeight = 13.sp)
        }
        if (deferred != null) {
            Text(deferred, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        } else if (granted) {
            Text("ALLOWED", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        } else if (onRequest != null) {
            Text("ALLOW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Violet).clickable { onRequest() }.padding(horizontal = 12.dp, vertical = 9.dp))
        } else {
            Text("NOT GRANTED", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun GalaxyProviderSettingsPanel() {
    val context = LocalContext.current
    val initial = remember { GalaxyAiProviderStore.load(context) }
    var baseUrl by remember { mutableStateOf(initial.baseUrl.ifBlank { "https://api.openai.com" }) }
    var model by remember { mutableStateOf(initial.model.ifBlank { "gpt-4o-mini" }) }
    var apiKey by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(initial.hasApiKey) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (initial.isConfigured) "Provider settings loaded. API key is encrypted on this device." else "No AI provider configured. Galaxy remains in offline mode.") }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Text("OPENAI-COMPATIBLE API", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("নিজের provider-এর HTTPS base URL, model ID এবং API key দাও। OpenAI-compatible Chat Completions endpoint সমর্থিত।", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
        Text("BASE URL", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        BasicTextField(value = baseUrl, onValueChange = { baseUrl = it.take(500) }, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Panel2).padding(12.dp))
        Spacer(Modifier.height(9.dp))
        Text("MODEL ID", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        BasicTextField(value = model, onValueChange = { model = it.take(120) }, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Panel2).padding(12.dp))
        Spacer(Modifier.height(9.dp))
        Text(if (hasKey) "API KEY • SAVED ENCRYPTED" else "API KEY", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        BasicTextField(value = apiKey, onValueChange = { apiKey = it.take(500) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Panel2).padding(12.dp),
            decorationBox = { inner -> Box { if (apiKey.isEmpty()) Text(if (hasKey) "Leave blank to keep saved key" else "Paste API key", color = Muted, fontSize = 11.sp); inner() } })
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SAVE CONFIG", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(Violet).clickable {
                    val saved = GalaxyAiProviderStore.save(context, baseUrl, model, apiKey)
                    if (saved) {
                        apiKey = ""
                        hasKey = GalaxyAiProviderStore.load(context).hasApiKey
                        status = if (hasKey) "Configuration saved. API key encrypted using Android Keystore." else "Configuration saved, but an API key is still required."
                    } else status = "Save failed. Use a valid HTTPS URL and a non-empty model ID."
                }.padding(horizontal = 10.dp, vertical = 12.dp))
            Text("TEST", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Panel2).clickable(enabled = !busy && GalaxyAiProviderStore.load(context).isConfigured) {
                    busy = true
                    status = "Testing provider connection…"
                    GalaxyAiClient.send(context, "You are running a connection test. Reply with exactly: Galaxy connection verified.") { result ->
                        busy = false
                        status = result.fold({ "Connection successful: ${it.take(100)}" }, { "Connection failed: ${it.message ?: "Unknown error"}" })
                    }
                }.padding(horizontal = 15.dp, vertical = 12.dp))
        }
        if (hasKey) {
            Text("REMOVE PROVIDER CONFIG", color = Color(0xFFFF9AAB), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 11.dp).clickable {
                    GalaxyAiProviderStore.clear(context)
                    hasKey = false
                    apiKey = ""
                    status = "Provider configuration and encrypted key removed from this device."
                }.padding(vertical = 5.dp))
        }
        Text(status, color = if (status.startsWith("Connection successful")) Color(0xFF8BE0B5) else Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 9.dp))
        Text("Security: HTTPS only • API key encrypted with Android Keystore • never written to source code or logs. Provider charges and data handling follow your provider's terms.", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun PersonalizationSettingsPanel() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(GalaxyPersonalizationStore.load(context)) }
    fun update(next: GalaxyPersonalization) {
        if (GalaxyPersonalizationStore.save(context, next)) settings = next
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Text("RESPONSE STYLE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GalaxyPersonalizationStore.responseStyles.forEach { option ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (settings.responseStyle == option) Color(0xFF29264F) else Panel2)
                    .semantics { role = Role.Button }.clickable { update(settings.copy(responseStyle = option)) }.padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (settings.responseStyle == option) "●" else "○", color = if (settings.responseStyle == option) Cyan else Muted, fontSize = 9.sp)
                    Text(option, color = if (settings.responseStyle == option) Cyan else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("CONVERSATION TONE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GalaxyPersonalizationStore.tones.forEach { option ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (settings.tone == option) Color(0xFF29264F) else Panel2)
                    .semantics { role = Role.Button }.clickable { update(settings.copy(tone = option)) }.padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (settings.tone == option) "●" else "○", color = if (settings.tone == option) Cyan else Muted, fontSize = 9.sp)
                    Text(option, color = if (settings.tone == option) Cyan else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("ASSISTANT MODE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GalaxyPersonalizationStore.modes.chunked(2).forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    group.forEach { option ->
                        val descriptions = mapOf("General" to "Everyday help", "Study" to "Learn step by step", "Coding" to "Build and debug", "Research" to "Evidence and methods")
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (settings.assistantMode == option) Color(0xFF29264F) else Panel2)
                            .semantics { role = Role.Button }.clickable { update(settings.copy(assistantMode = option)) }.padding(10.dp)) {
                            Text(if (settings.assistantMode == option) "● $option" else "○ $option", color = if (settings.assistantMode == option) Cyan else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(descriptions[option] ?: "", color = Muted, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SettingsToggleRow("Preference learning", "Off by default. Galaxy must ask before saving a new preference.", settings.learningEnabled) {
            update(settings.copy(learningEnabled = it))
        }
        Text("সব সেটিংস এই ডিভাইসে সংরক্ষিত। এগুলো ভবিষ্যৎ AI Core-এর prompt guidance তৈরি করে; এই build-এ বাস্তব AI response বদলায় না, কারণ AI provider এখনো connected নয়।", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun SettingsToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(description, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BottomNavigation(selected: GalaxyTab, onSelect: (GalaxyTab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF090B17)).padding(horizontal = 8.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
        listOf(GalaxyTab.HOME, GalaxyTab.CHAT, GalaxyTab.LAB, GalaxyTab.MEMORY, GalaxyTab.PROFILE).forEach { tab ->
            val active = tab == selected
            Column(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).heightIn(min = 48.dp).semantics { contentDescription = "${tab.label} navigation tab"; role = Role.Tab; this.selected = active }.clickable { onSelect(tab) }.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tab.glyph, color = if (active) Cyan else Muted)
                Spacer(Modifier.height(3.dp))
                Text(tab.label, color = if (active) Color.White else Muted, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                if (active) {
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.width(15.dp).height(2.dp).clip(CircleShape).background(Cyan))
                }
            }
        }
    }
}

@Composable
private fun StarBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stars = listOf(0.06f to 0.12f, 0.91f to 0.10f, 0.18f to 0.31f, 0.86f to 0.42f, 0.08f to 0.66f, 0.94f to 0.75f, 0.63f to 0.22f, 0.42f to 0.86f, 0.72f to 0.92f, 0.30f to 0.52f, 0.52f to 0.08f)
        stars.forEachIndexed { index, p ->
            drawCircle(if (index % 3 == 0) Cyan.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.55f), if (index % 4 == 0) 2.0f else 1.1f, Offset(p.first * size.width, p.second * size.height))
        }
    }
}
