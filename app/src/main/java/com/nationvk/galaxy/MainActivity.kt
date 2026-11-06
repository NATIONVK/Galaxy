package com.nationvk.galaxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GalaxyApp() }
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
fun GalaxyApp() {
    var selected by remember { mutableStateOf(GalaxyTab.HOME) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Space) {
            Box(Modifier.fillMaxSize()) {
                StarBackdrop(Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize()) {
                    if (selected != GalaxyTab.BLACKHOLE) GalaxyTopBar()
                    Box(Modifier.weight(1f)) {
                        when (selected) {
                            GalaxyTab.BLACKHOLE -> BlackHoleExperience(onBack = { selected = GalaxyTab.HOME })
                            GalaxyTab.HOME -> HomeScreen(onOpen = { selected = it })
                            GalaxyTab.CHAT -> ChatScreen()
                            GalaxyTab.LAB -> FeatureScreen("CYBER LAB", "নিরাপদে শেখো, পরীক্ষা করো", "Your personal learning & security workspace.", "Practice area, learning paths and local sandbox tools will be built here.", "Explore learning paths", "Lab tools are not connected yet")
                            GalaxyTab.MEMORY -> FeatureScreen("MEMORY VAULT", "তোমার জ্ঞান, এক জায়গায়", "A private space for saved context and notes.", "Memory controls, searchable notes and personal preferences will be added in a later phase.", "Open memory vault", "Memory storage is not active yet")
                            GalaxyTab.PROFILE -> ProfileScreen(onSettings = { selected = GalaxyTab.SETTINGS })
                            GalaxyTab.VOICE -> VoiceStudioScreen()
                            GalaxyTab.RESEARCH -> FeatureScreen("RESEARCH HUB", "Research & Data Analysis", "Turn questions into structured insights.", "Workspace layout for research projects, datasets, analysis tools, charts and saved reports. Data processing engine will be added later.", "Create research workspace", "Analysis engine not connected")
                            GalaxyTab.CAMERA -> FeatureScreen("VISION STUDIO", "Camera & Image Understanding", "See, capture and explore.", "Camera screen layout for live preview, capture action, image upload and analysis history. Camera permission and vision model are not connected yet.", "Open camera preview", "Camera service not connected")
                            GalaxyTab.MODELS -> ModelControlScreen()
                            GalaxyTab.NOTIFICATIONS -> FeatureScreen("NOTIFICATION CENTER", "Your universe updates", "Important activity in one place.", "Notification inbox layout for assistant updates, reminders, system messages and security events. Push notifications are not configured.", "Notification preferences", "Notifications are not enabled")
                            GalaxyTab.SETTINGS -> SettingsScreen()
                        }
                    }
                    if (selected != GalaxyTab.BLACKHOLE) BottomNavigation(selected = selected, onSelect = { selected = it })
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
            Text("PERSONAL AI UNIVERSE", color = Cyan, fontSize = 8.sp, letterSpacing = 2.sp)
        }
        Box(Modifier.clip(CircleShape).background(Color(0xFF14283A)).padding(horizontal = 11.dp, vertical = 7.dp)) {
            Text("●  UI FINALIZATION", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeScreen(onOpen: (GalaxyTab) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WELCOME TO YOUR UNIVERSE", color = Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
                Text("Hello, Explorer.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.clip(CircleShape).background(Panel2).padding(11.dp)) { Text("✧", color = Violet, fontSize = 21.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(390.dp).clip(RoundedCornerShape(25.dp)).clickable { onOpen(GalaxyTab.BLACKHOLE) }) {
            GalaxyGLSurfaceViewHost(Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD050611)))).padding(18.dp)) {
                Column {
                    Text("SINGULARITY", color = Cyan, fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Text("Explore the unknown.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionHeading("YOUR AI UNIVERSE", "Explore modules")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            ModuleCard("✧", "AI CHAT", "Talk with Galaxy", Modifier.weight(1f)) { onOpen(GalaxyTab.CHAT) }
            ModuleCard("⌘", "CYBER LAB", "Learn & experiment", Modifier.weight(1f)) { onOpen(GalaxyTab.LAB) }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            ModuleCard("◈", "MEMORY", "Your knowledge vault", Modifier.weight(1f)) { onOpen(GalaxyTab.MEMORY) }
            ModuleCard("⌁", "RESEARCH", "Analysis workspace", Modifier.weight(1f)) { onOpen(GalaxyTab.RESEARCH) }
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
            ModuleCard("⌁", "RESEARCH", "Data & reports", Modifier.weight(1f)) { onOpen(GalaxyTab.RESEARCH) }
            ModuleCard("⎇", "AI MODELS", "Provider control", Modifier.weight(1f)) { onOpen(GalaxyTab.MODELS) }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            ModuleCard("♢", "NOTIFICATIONS", "Activity inbox", Modifier.weight(1f)) { onOpen(GalaxyTab.NOTIFICATIONS) }
            ModuleCard("⚙", "SETTINGS", "App preferences", Modifier.weight(1f)) { onOpen(GalaxyTab.SETTINGS) }
        }
        Spacer(Modifier.height(20.dp))
        SectionHeading("SYSTEM STATUS", "Foundation phase")
        Spacer(Modifier.height(10.dp))
        StatusPanel()
        Spacer(Modifier.height(18.dp))
        Text("BUILT FOR CURIOSITY  •  NATIONVK", color = Color(0xFF606985), fontSize = 9.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(14.dp))
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
            Text("SELECTION MODE", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionModeCard("AUTO", "Smart routing", "Galaxy chooses by task", mode == "AUTO", Modifier.weight(1f)) { mode = "AUTO" }
                SelectionModeCard("MANUAL", "Your choice", "You select the model", mode == "MANUAL", Modifier.weight(1f)) { mode = "MANUAL" }
            }
            Spacer(Modifier.height(12.dp))
            Text(if (mode == "AUTO") "Automatic mode will select an available model based on task type and capability. Routing activates after Core integration." else "Manual mode lets you choose a preferred model. The selection below is currently a UI preference preview.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("MODEL DIRECTORY", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        models.forEachIndexed { index, model ->
            val active = selectedModel == model
            Row(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(16.dp)).background(if (active) Color(0xFF202447) else Panel).clickable { if (mode == "MANUAL") selectedModel = model }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (active) Color(0x443FBCFF) else Panel2), contentAlignment = Alignment.Center) {
                    Text(listOf("✦", "⚡", "◈", "◉")[index], color = if (active) Cyan else Violet, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(model, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (index == 3) "Image and document tasks" else if (index == 2) "Complex multi-step tasks" else if (index == 1) "Quick everyday responses" else "Default assistant profile", color = Muted, fontSize = 10.sp)
                }
                Text(if (active) "SELECTED" else if (mode == "AUTO") "AUTO" else "CHOOSE", color = if (active) Cyan else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(if (active) Color(0xFF25294F) else Panel2).clickable(onClick = onClick).padding(12.dp)) {
        Text(if (active) "●  $title" else "○  $title", color = if (active) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(heading, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun BlackHoleExperience(onBack: () -> Unit) {
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var intensity by remember { mutableFloatStateOf(1.0f) }
    var particles by remember { mutableFloatStateOf(0.65f) }
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
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(30.dp)).background(Color(0xAA101326)).clickable(onClick = onBack).padding(horizontal = 16.dp, vertical = 11.dp)) {
                    Text("←  BACK TO GALAXY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text("LIVE VISUAL", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xCC080B1B)).padding(15.dp)) {
                Text("THE SINGULARITY", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text("LIVE VISUAL CONTROLS", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp)
                Text("Pinch to zoom · Drag to move", color = Muted, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                Text("Zoom  ${String.format("%.1f", zoom)}×", color = Color.White, fontSize = 11.sp)
                Slider(value = zoom, onValueChange = { zoom = it; glView?.updateVisuals(zoom, speed, intensity, particles) }, valueRange = 0.7f..1.8f)
                Text("Animation speed  ${String.format("%.1f", speed)}×", color = Color.White, fontSize = 11.sp)
                Slider(value = speed, onValueChange = { speed = it; glView?.updateVisuals(zoom, speed, intensity, particles) }, valueRange = 0.2f..2.5f)
                Text("Glow intensity  ${String.format("%.1f", intensity)}×", color = Color.White, fontSize = 11.sp)
                Slider(value = intensity, onValueChange = { intensity = it; glView?.updateVisuals(zoom, speed, intensity, particles) }, valueRange = 0.3f..2.0f)
                Text("Particle density  ${String.format("%.0f", particles * 100)}%", color = Color.White, fontSize = 11.sp)
                Slider(value = particles, onValueChange = { particles = it; glView?.updateVisuals(zoom, speed, intensity, particles) }, valueRange = 0.1f..1.0f)
            }
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
    Column(modifier.clip(RoundedCornerShape(19.dp)).background(Brush.linearGradient(listOf(Panel2, Panel))).clickable(onClick = onClick).padding(15.dp)) {
        Box(Modifier.size(35.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x332F9DFF)), contentAlignment = Alignment.Center) {
            Text(icon, color = Cyan, fontSize = 21.sp)
        }
        Spacer(Modifier.height(13.dp))
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(description, color = Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Text("OPEN  ↗", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Pair<Boolean, String>>()) }
    var attachmentOpen by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GALAXY CONVERSATION", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("AI Chat", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Text("DEMO UI", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(Panel2).padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = Cyan, fontSize = 18.sp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Galaxy Assistant", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Conversation interface preview • AI Core not connected", color = Muted, fontSize = 9.sp)
            }
            Text("● OFFLINE", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(Panel2).clickable { draft = suggestion }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✧", color = Violet, fontSize = 13.sp)
                        Spacer(Modifier.width(9.dp))
                        Text(suggestion, color = Color(0xFFDCE2F7), fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text("↗", color = Cyan, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 5.dp)) {
                messages.forEach { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.first) Arrangement.End else Arrangement.Start) {
                        Column(Modifier.widthIn(max = 290.dp).padding(vertical = 5.dp).clip(RoundedCornerShape(17.dp)).background(if (message.first) Color(0xFF29264F) else Panel).padding(13.dp)) {
                            Text(if (message.first) "YOU" else "LOCAL NOTE", color = if (message.first) Cyan else Violet, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(message.second, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
                Text("Messages are stored in this screen only. No AI reply is generated in demo mode.", color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
        if (attachmentOpen) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(13.dp)).background(Panel2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("＋", color = Cyan, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("Attachments will be enabled with file and vision services.", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("CLOSE", color = Cyan, fontSize = 9.sp, modifier = Modifier.clickable { attachmentOpen = false })
            }
        }
        if (notice.isNotBlank()) {
            Text(notice, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(start = 5.dp, bottom = 6.dp))
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel2).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Panel).clickable { attachmentOpen = !attachmentOpen }, contentAlignment = Alignment.Center) {
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
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (draft.isBlank()) Panel else Violet).clickable {
                val value = draft.trim()
                if (value.isNotEmpty()) {
                    messages = messages + (true to value)
                    draft = ""
                    notice = "Saved locally in this screen — AI response unavailable in demo mode."
                }
            }, contentAlignment = Alignment.Center) {
                Text("↑", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Demo mode • local screen state only", color = Muted, fontSize = 8.sp, modifier = Modifier.weight(1f))
            Text("CLEAR CHAT", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                messages = emptyList()
                notice = "Local conversation cleared."
            }.padding(5.dp))
        }
    }
}

@Composable
private fun VoiceStudioScreen() {
    var voiceMode by remember { mutableStateOf("LISTEN") }
    var selectedVoice by remember { mutableStateOf("Nova") }
    var transcript by remember { mutableStateOf("") }
    var showVoices by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GALAXY AUDIO EXPERIENCE", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Voice Studio", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("কথা বলার অভিজ্ঞতার জন্য প্রস্তুত workspace", color = Muted, fontSize = 11.sp)
            }
            Text("UI PREVIEW", color = Violet, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(Panel2).padding(horizontal = 10.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.radialGradient(listOf(Color(0xFF34306C), Panel, Space))).padding(vertical = 24.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(166.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0x6655DFFF), Color(0x449B69FF), Color(0x11101326)))), contentAlignment = Alignment.Center) {
                Box(Modifier.size(124.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF51418D), Color(0xFF171B33), Color(0xFF101326)))), contentAlignment = Alignment.Center) {
                    Text("♫", color = Cyan, fontSize = 54.sp)
                }
            }
            Spacer(Modifier.height(15.dp))
            Text(if (voiceMode == "LISTEN") "Ready to listen" else if (voiceMode == "SPEAK") "Speech preview selected" else "Voice paused", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Microphone and speech engine are not connected", color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("LISTEN", "SPEAK", "PAUSE").forEach { mode ->
                    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(if (voiceMode == mode) Color(0xFF29264F) else Panel2).clickable { voiceMode = mode }.padding(horizontal = 15.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (voiceMode == mode) "● $mode" else mode, color = if (voiceMode == mode) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF14283A)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 10.sp)
            Spacer(Modifier.width(9.dp))
            Text("Preview controls only — no audio is recorded, played or uploaded.", color = Color(0xFFB8D8E8), fontSize = 10.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("VOICE PROFILE", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("CHANGE", color = Muted, fontSize = 9.sp, modifier = Modifier.clickable { showVoices = !showVoices })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).clickable { showVoices = !showVoices }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) { Text("✧", color = Violet, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(selectedVoice, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Cosmic voice profile • demo preset", color = Muted, fontSize = 10.sp)
            }
            Text(if (showVoices) "⌃" else "⌄", color = Cyan, fontSize = 18.sp)
        }
        if (showVoices) {
            listOf("Nova", "Orion", "Luna", "Atlas").forEach { voice ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp)).background(Panel2).clickable { selectedVoice = voice; showVoices = false }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(voice, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(if (selectedVoice == voice) "SELECTED" else "SELECT", color = if (selectedVoice == voice) Cyan else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("TRANSCRIPT / TEXT INPUT", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).padding(14.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value = transcript,
                onValueChange = { transcript = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 74.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp, lineHeight = 18.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                decorationBox = { inner ->
                    Box {
                        if (transcript.isEmpty()) Text("এখানে কথার transcript বা text লিখে রাখো...", color = Muted, fontSize = 11.sp)
                        inner()
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LOCAL DRAFT ONLY", color = Muted, fontSize = 8.sp, modifier = Modifier.weight(1f))
                Text("CLEAR", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { transcript = "" }.padding(5.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).padding(15.dp)) {
            Text("AUDIO SETTINGS", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Speech speed", color = Color.White, fontSize = 11.sp)
            var speed by remember { mutableFloatStateOf(1f) }
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2f)
            Text("Speed preference: ${String.format("%.1f", speed)}× • demo only", color = Muted, fontSize = 9.sp)
        }
        Spacer(Modifier.height(12.dp))
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
            Text("MODULE OVERVIEW", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text("YOUR SPACE", color = Cyan, fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Explorer Profile", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Color(0xFF163E67)))), contentAlignment = Alignment.Center) {
                Text("N", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            Text("Galaxy Explorer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Personal AI workspace", color = Muted, fontSize = 11.sp)
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
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                    language = option
                    prefs.edit().putString("language", option).apply()
                }.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(option, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(if (language == option) "● SELECTED" else "○", color = if (language == option) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Preference saved on this device. Full interface localization will be applied in a later pass.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
        }

        Spacer(Modifier.height(18.dp))
        SettingsSectionTitle("PRIVACY & SECURITY")
        SettingsInfoRow("App permissions", "No permissions requested by this screen")
        SettingsInfoRow("Memory controls", "Memory service not connected")
        SettingsInfoRow("Data & privacy", "Local preferences stay on this device")
        Spacer(Modifier.height(18.dp))
        SettingsSectionTitle("AI PREFERENCES")
        SettingsToggleRow("Web access preference", "Saved preference only; web tools are not connected", webPreference) {
            webPreference = it
            prefs.edit().putBoolean("web_preference", it).apply()
        }
        SettingsInfoRow("Preferred model", "Choose from AI Models • demo only")
        SettingsInfoRow("Voice preferences", "Voice service not connected")
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
private fun SettingsSectionTitle(title: String) {
    Text(title, color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { onSelect(tab) }.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tab.glyph, color = if (active) Cyan else Muted, fontSize = 20.sp)
                Spacer(Modifier.height(3.dp))
                Text(tab.label, color = if (active) Color.White else Muted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
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
