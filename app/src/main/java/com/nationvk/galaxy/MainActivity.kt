package com.nationvk.galaxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
                            GalaxyTab.LAB -> CyberLabScreen()
                            GalaxyTab.MEMORY -> MemoryVaultScreen()
                            GalaxyTab.PROFILE -> ProfileScreen(onSettings = { selected = GalaxyTab.SETTINGS })
                            GalaxyTab.VOICE -> VoiceStudioScreen()
                            GalaxyTab.RESEARCH -> ResearchHubScreen()
                            GalaxyTab.CAMERA -> VisionStudioScreen()
                            GalaxyTab.MODELS -> ModelControlScreen()
                            GalaxyTab.NOTIFICATIONS -> NotificationCenterScreen()
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
private fun MemoryVaultScreen() {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }
    var noteDraft by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var showEditor by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val categories = listOf("ALL", "NOTES", "IDEAS", "RESEARCH", "LEARNING")
    val filtered = notes.filter { (category, content) ->
        (selectedCategory == "ALL" || selectedCategory == category) && content.contains(search, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YOUR PERSONAL KNOWLEDGE SPACE", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("Memory Vault", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Notes, ideas and saved context — organized.", color = Muted, fontSize = 10.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("◈", color = Violet, fontSize = 23.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⌕", color = Cyan, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                decorationBox = { inner ->
                    Box {
                        if (search.isEmpty()) Text("Search your vault...", color = Muted, fontSize = 11.sp)
                        inner()
                    }
                }
            )
            if (search.isNotEmpty()) Text("×", color = Muted, fontSize = 18.sp, modifier = Modifier.clickable { search = "" })
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            categories.forEach { category ->
                val active = selectedCategory == category
                Text(category, color = if (active) Color.White else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (active) Color(0xFF302A5B) else Panel).clickable { selectedCategory = category }.padding(horizontal = 10.dp, vertical = 9.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YOUR COLLECTION", color = Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("${filtered.size} local item(s)", color = Muted, fontSize = 9.sp)
            }
            Text("+ NEW NOTE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Violet).clickable { showEditor = !showEditor }.padding(horizontal = 12.dp, vertical = 10.dp))
        }
        if (showEditor) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel2).padding(13.dp)) {
                Text("CREATE LOCAL NOTE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NOTES", "IDEAS", "RESEARCH", "LEARNING").forEach { category ->
                        Text(category, color = if (selectedCategory == category) Cyan else Muted, fontSize = 7.sp,
                            modifier = Modifier.clickable { selectedCategory = category }.padding(vertical = 4.dp))
                    }
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 65.dp).padding(vertical = 8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp, lineHeight = 17.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan),
                    decorationBox = { inner ->
                        Box {
                            if (noteDraft.isEmpty()) Text("Write a thought, idea or learning note...", color = Muted, fontSize = 10.sp)
                            inner()
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCEL", color = Muted, fontSize = 9.sp, modifier = Modifier.clickable { showEditor = false; noteDraft = "" }.padding(8.dp))
                    Text("SAVE NOTE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        val body = noteDraft.trim()
                        if (body.isNotEmpty()) {
                            val category = if (selectedCategory in categories.drop(1)) selectedCategory else "NOTES"
                            notes = listOf(category to body) + notes
                            noteDraft = ""
                            showEditor = false
                            selectedCategory = "ALL"
                            notice = "Note saved in this screen's temporary state."
                        }
                    }.padding(8.dp))
                }
            }
        }
        if (notice.isNotBlank()) {
            Text(notice, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 7.dp))
        }
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.radialGradient(listOf(Color(0xFF25234D), Panel, Space))).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("◈", color = Violet, fontSize = 58.sp)
                Spacer(Modifier.height(12.dp))
                Text(if (search.isEmpty()) "Your vault is waiting" else "No matching memories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(if (search.isEmpty()) "Create a note to start collecting your knowledge. Your notes are currently temporary and will reset when this screen is recreated." else "Try another keyword or category.", color = Muted, fontSize = 10.sp, lineHeight = 16.sp)
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                filtered.forEach { item ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.first, color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                            Text("LOCAL", color = Muted, fontSize = 7.sp)
                            Text("  ×", color = Muted, fontSize = 15.sp, modifier = Modifier.clickable { notes = notes - item }.padding(start = 8.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.second, color = Color(0xFFE2E6F5), fontSize = 11.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFF14283A)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 9.sp)
            Spacer(Modifier.width(8.dp))
            Text("Demo vault: temporary screen state only. Persistent storage, synced memory and AI recall are not connected.", color = Color(0xFFB8D8E8), fontSize = 9.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun CyberLabScreen() {
    var activeSection by remember { mutableStateOf("OVERVIEW") }
    var selectedPath by remember { mutableStateOf("Cybersecurity Foundations") }
    var completed by remember { mutableStateOf(setOf<String>()) }
    var sandboxTarget by remember { mutableStateOf("Local practice environment") }
    var sandboxStatus by remember { mutableStateOf("IDLE") }
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
                Text("DEFENSIVE SECURITY WORKSPACE", color = Cyan, fontSize = 9.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
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
                Text("LEARNING PROGRESS", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("${completed.size} / ${paths.size} paths completed", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Progress is stored only while this screen remains active.", color = Muted, fontSize = 9.sp)
            }
            Box(Modifier.size(58.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("${(completed.size * 100 / paths.size)}%", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("OVERVIEW", "PATHS", "SANDBOX", "TOOLS").forEach { section ->
                val active = activeSection == section
                Text(section, color = if (active) Color.White else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (active) Color(0xFF302A5B) else Color.Transparent).clickable { activeSection = section }.padding(vertical = 11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(15.dp))
        when (activeSection) {
            "OVERVIEW" -> {
                Text("WELCOME TO THE LAB", color = Cyan, fontSize = 9.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text("Build skills the safe way.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("A guided workspace for defensive security, digital safety and practical fundamentals.", color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf("LEARNING PATHS" to "5 guided tracks", "SAFE SANDBOX" to "Local demo only").forEach { item ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Panel).padding(13.dp)) {
                            Text("✧", color = Violet, fontSize = 20.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(item.first, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(item.second, color = Muted, fontSize = 9.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("CONTINUE LEARNING", color = Cyan, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                paths.take(3).forEach { path ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).clickable { selectedPath = path.first; activeSection = "PATHS" }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(path.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(path.second, color = Muted, fontSize = 9.sp)
                        }
                        Text(if (completed.contains(path.first)) "DONE" else "OPEN  ›", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            "PATHS" -> {
                Text("GUIDED LEARNING PATHS", color = Cyan, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Choose a track and mark it complete when you finish.", color = Muted, fontSize = 10.sp)
                paths.forEach { path ->
                    val done = completed.contains(path.first)
                    Column(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(16.dp)).background(if (selectedPath == path.first) Color(0xFF222448) else Panel).clickable { selectedPath = path.first }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (done) "✓" else "◈", color = if (done) Cyan else Violet, fontSize = 19.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(path.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(path.second, color = Muted, fontSize = 9.sp)
                            }
                            Text(if (done) "DONE" else if (selectedPath == path.first) "SELECTED" else "OPEN", color = if (done) Cyan else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        if (selectedPath == path.first) {
                            Spacer(Modifier.height(10.dp))
                            Text("Suggested lessons: Introduction • Key terms • Safe practice • Knowledge check", color = Color(0xFFD1D8EF), fontSize = 9.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (done) "MARKED COMPLETE" else "MARK PATH COMPLETE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { completed = if (done) completed - path.first else completed + path.first }.padding(vertical = 6.dp))
                        }
                    }
                }
            }
            "SANDBOX" -> {
                Text("SAFE SANDBOX", color = Cyan, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Practice without touching real systems.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("This is a visual simulator only. It does not run commands, scan networks or connect to external targets.", color = Muted, fontSize = 10.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(13.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color(0xFF080D18)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("LOCAL LAB CONSOLE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("● ${sandboxStatus}", color = if (sandboxStatus == "IDLE") Muted else Cyan, fontSize = 8.sp)
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("SELECT PRACTICE MODULE", color = Muted, fontSize = 8.sp)
                    listOf("Local practice environment", "Sample log analysis", "HTTP request walkthrough").forEach { target ->
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(10.dp)).background(if (sandboxTarget == target) Color(0xFF20294A) else Panel).clickable { sandboxTarget = target; sandboxStatus = "IDLE" }.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        Text("RUN PREVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Violet).clickable { sandboxStatus = "PREVIEW" }.padding(horizontal = 13.dp, vertical = 10.dp))
                        Text("RESET", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Panel2).clickable { sandboxStatus = "IDLE" }.padding(horizontal = 13.dp, vertical = 10.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Safety boundary: no exploit payloads, credential attacks, unauthorized scanning or real target execution.", color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
            }
            "TOOLS" -> {
                Text("DEFENSIVE TOOLS DIRECTORY", color = Cyan, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Learning aids and safe sample-data utilities.", color = Muted, fontSize = 10.sp)
                tools.forEach { tool ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(35.dp).clip(RoundedCornerShape(11.dp)).background(Panel2), contentAlignment = Alignment.Center) { Text("⌘", color = Cyan, fontSize = 17.sp) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tool.first, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(tool.second, color = Muted, fontSize = 8.sp)
                        }
                        Text("GUIDE", color = Violet, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0xFF14283A)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 9.sp)
            Spacer(Modifier.width(8.dp))
            Text("UI preview only • no live cyber operations or external connections.", color = Color(0xFFB8D8E8), fontSize = 9.sp, lineHeight = 14.sp)
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
                Text("DATA • STATISTICS • DISCOVERY", color = Cyan, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
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
                Text("RESEARCH WORKSPACE", color = Cyan, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("3 sample datasets", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("1 project draft • demo charts", color = Muted, fontSize = 9.sp)
            }
            Text("✦", color = Violet, fontSize = 32.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Panel).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("OVERVIEW", "DATASETS", "ANALYSIS", "PROJECTS", "REPORT").forEach { tab ->
                Text(tab, color = if (section == tab) Color.White else Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (section == tab) Color(0xFF302A5B) else Color.Transparent).clickable { section = tab }.padding(vertical = 11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(15.dp))
        when (section) {
            "OVERVIEW" -> {
                Text("YOUR RESEARCH DESK", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("From raw data to clear insight.", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("A focused workspace for organizing datasets, planning analysis and preparing research reports.", color = Muted, fontSize = 10.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DATASETS" to "03", "PROJECTS" to "01", "REPORTS" to "01").forEach { item ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(Panel).padding(11.dp)) {
                            Text(item.second, color = Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(item.first, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text("RECENT DATASET", color = Cyan, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(Panel).clickable { section = "DATASETS" }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedDataset, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Sample data • 4 observations • 3 variables", color = Muted, fontSize = 9.sp)
                    }
                    Text("OPEN  ›", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(15.dp))
                Text("QUICK ANALYSIS", color = Cyan, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                listOf("Descriptive statistics", "Correlation overview", "Distribution chart").forEach { label ->
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(12.dp)).background(Panel).clickable { section = "ANALYSIS" }.padding(12.dp)) {
                        Text("◈", color = Violet, fontSize = 15.sp)
                        Spacer(Modifier.width(9.dp))
                        Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text("VIEW", color = Muted, fontSize = 8.sp)
                    }
                }
            }
            "DATASETS" -> {
                Text("DATASET WORKSPACE", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Manage your research data.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                datasets.forEach { dataset ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 7.dp).clip(RoundedCornerShape(12.dp)).background(if (selectedDataset == dataset) Color(0xFF25294F) else Panel).clickable { selectedDataset = dataset }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("▦", color = Cyan, fontSize = 18.sp)
                        Spacer(Modifier.width(9.dp))
                        Text(dataset, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text(if (selectedDataset == dataset) "ACTIVE" else "SELECT", color = if (selectedDataset == dataset) Cyan else Muted, fontSize = 8.sp)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(13.dp)) {
                    Text("IMPORT CSV FILE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("CSV import interface preview. File picker and parsing will be connected in a later data-engine phase.", color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(if (importedFile.isEmpty()) "＋  CHOOSE CSV FILE" else "✓  $importedFile", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Violet).clickable { importedFile = "sample_research_data.csv (demo)" }.padding(horizontal = 13.dp, vertical = 10.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("DATA PREVIEW • $selectedDataset", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(8.dp)) {
                    sampleRows.forEachIndexed { index, cells ->
                        Row(Modifier.fillMaxWidth().background(if (index == 0) Panel2 else Color.Transparent).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            cells.forEach { cell -> Text(cell, color = if (index == 0) Cyan else Color(0xFFDDE2F2), fontSize = 8.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                        }
                    }
                }
            }
            "ANALYSIS" -> {
                Text("STATISTICAL ANALYSIS", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Explore the sample indicators.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Illustrative values only — calculations are not connected to a live dataset yet.", color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mean study" to "3.63 h", "Mean GPA" to "3.59", "Mean sleep" to "7.13 h").forEach { item ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(Panel).padding(10.dp)) {
                            Text(item.first, color = Muted, fontSize = 8.sp)
                            Text(item.second, color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("STUDY HOURS BY SAMPLE STUDENT", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                Text("DESCRIPTIVE SUMMARY", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                listOf("Study hours" to "Mean 3.63 • Min 2.0 • Max 5.0", "Sleep hours" to "Mean 7.13 • Min 6.0 • Max 8.0", "GPA" to "Mean 3.59 • Min 3.20 • Max 3.85").forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(11.dp)).background(Panel).padding(11.dp)) {
                        Text(row.first, color = Color.White, fontSize = 9.sp, modifier = Modifier.weight(1f))
                        Text(row.second, color = Muted, fontSize = 8.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Planned methods: frequency tables • mean/median • standard deviation • correlation • linear regression.", color = Muted, fontSize = 9.sp, lineHeight = 15.sp)
            }
            "PROJECTS" -> {
                Text("RESEARCH PROJECTS", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Organize your investigations.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(13.dp)) {
                    Text("NEW PROJECT NAME", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(projectName, color = Color.White, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Panel2).padding(12.dp))
                    Text("Tap below to cycle through suggested project titles.", color = Muted, fontSize = 8.sp)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("EDIT TITLE", color = Color.White, fontSize = 8.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).clickable { projectName = if (projectName.startsWith("Study")) "Campus Attendance & GPA" else "Study Time & GPA Analysis" }.padding(10.dp))
                        Text("CREATE PROJECT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Violet).clickable { projectCreated = true }.padding(10.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Panel).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (projectCreated) projectName else "Study Time & GPA Analysis", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(if (projectCreated) "Draft created in this screen session" else "Draft workspace • sample project", color = Muted, fontSize = 9.sp)
                    }
                    Text(if (projectCreated) "DRAFT" else "SAMPLE", color = Cyan, fontSize = 8.sp)
                }
                Text("Project state is temporary UI state; cloud/database saving is not active.", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 9.dp))
            }
            "REPORT" -> {
                Text("REPORT PREVIEW", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Research summary draft.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F1F8)).padding(16.dp)) {
                    Text("RESEARCH REPORT", color = Color(0xFF252A48), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Study Time, Sleep Duration and GPA", color = Color(0xFF171A2C), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Sample analysis • Draft preview", color = Color(0xFF62677D), fontSize = 9.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("1. Objective", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Explore illustrative relationships between study habits and academic performance.", color = Color(0xFF44495D), fontSize = 9.sp, lineHeight = 14.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("2. Dataset", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Four fictional observations with study hours, sleep hours and GPA.", color = Color(0xFF44495D), fontSize = 9.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("3. Preliminary findings", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Descriptive values are shown for interface demonstration only. No inferential conclusion should be drawn.", color = Color(0xFF44495D), fontSize = 9.sp, lineHeight = 14.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("4. Limitations", color = Color(0xFF252A48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Synthetic sample; not suitable for academic claims or publication.", color = Color(0xFF44495D), fontSize = 9.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(if (reportPreview) "Preview refreshed • no file exported" else "Preview is a visual draft; PDF/export is not connected.", color = Muted, fontSize = 9.sp)
                Text("REFRESH PREVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(9.dp)).background(Violet).clickable { reportPreview = true }.padding(horizontal = 13.dp, vertical = 10.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF14283A)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 9.sp)
            Spacer(Modifier.width(8.dp))
            Text("UI preview only • no live data processing, file parsing or AI Core connection.", color = Color(0xFFB8D8E8), fontSize = 9.sp, lineHeight = 14.sp)
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
                Text("CAMERA • IMAGES • VISUAL INTELLIGENCE", color = Cyan, fontSize = 8.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("Vision Studio", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Capture, organize and explore visual content.", color = Muted, fontSize = 10.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                Text("◉", color = Cyan, fontSize = 23.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Panel).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("CAMERA", "UPLOAD", "GALLERY", "ANALYSIS").forEach { tab ->
                Text(tab, color = if (activeTab == tab) Color.White else Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (activeTab == tab) Color(0xFF302A5B) else Color.Transparent).clickable { activeTab = tab }.padding(vertical = 11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(14.dp))
        when (activeTab) {
            "CAMERA" -> {
                Text("CAMERA PREVIEW", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
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
                        Text("◉", color = Cyan, fontSize = 42.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("CAMERA PREVIEW PLACEHOLDER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Camera permission and hardware preview are not connected.", color = Muted, fontSize = 8.sp)
                    }
                    Text("● ${cameraState}", color = if (cameraState == "READY") Cyan else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(13.dp))
                    Text("16:9  •  PREVIEW", color = Muted, fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(13.dp))
                }
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("START PREVIEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Violet).clickable { cameraState = "DEMO" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("CAPTURE DEMO", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Panel2).clickable { selectedImage = "New demo capture • preview.png"; uploadState = "Demo capture selected"; activeTab = "ANALYSIS" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                Text("CAMERA STATES", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                    listOf("READY", "LOADING", "ERROR").forEach { state ->
                        Text(state, color = if (cameraState == state) Color.White else Muted, fontSize = 8.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (cameraState == state) Color(0xFF302A5B) else Panel).clickable { cameraState = state }.padding(horizontal = 12.dp, vertical = 9.dp))
                    }
                }
                if (cameraState == "ERROR") VisionStateCard("Camera unavailable", "Camera access is not configured in this build. Check permission and device support after integration.", "ERROR")
                if (cameraState == "LOADING") VisionStateCard("Preparing preview…", "This is a loading-state design preview. No camera service is starting.", "LOADING")
            }
            "UPLOAD" -> {
                Text("IMAGE UPLOAD", color = Cyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text("Bring an image into your workspace.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF252A55), Color(0xFF101426)))), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⇧", color = Cyan, fontSize = 38.sp)
                            Text("SELECT IMAGE FILE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("JPG • PNG • WEBP", color = Muted, fontSize = 8.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("＋  CHOOSE IMAGE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Violet).clickable { uploadState = "Demo image selected"; selectedImage = "Uploaded sample • image.png"; showError = false }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(uploadState, color = if (uploadState.startsWith("No")) Muted else Cyan, fontSize = 9.sp)
                    Text("File picker and real image decoding will be connected in a later integration phase.", color = Muted, fontSize = 8.sp, lineHeight = 13.sp)
                }
                Spacer(Modifier.height(11.dp))
                Text("UPLOAD VALIDATION STATES", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 7.dp)) {
                    Text("SIMULATE ERROR", color = Color.White, fontSize = 8.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).clickable { showError = true }.padding(10.dp))
                    Text("RESET", color = Muted, fontSize = 8.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Panel2).clickable { showError = false; uploadState = "No file selected" }.padding(10.dp))
                }
                if (showError) VisionStateCard("Unsupported file", "This demo state represents an invalid format or file that could not be read.", "ERROR")
            }
            "GALLERY" -> {
                Text("IMAGE GALLERY & HISTORY", color = Cyan, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("Your recent visual workspace.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SAMPLE ITEMS", color = Muted, fontSize = 8.sp, modifier = Modifier.weight(1f).padding(top = 9.dp))
                    Text(if (showGalleryEmpty) "SHOW ITEMS" else "EMPTY STATE", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showGalleryEmpty = !showGalleryEmpty }.padding(8.dp))
                }
                if (showGalleryEmpty) {
                    VisionStateCard("No images yet", "Your uploaded images and captures will appear here once the gallery storage is connected.", "EMPTY")
                } else {
                    gallery.chunked(2).forEachIndexed { rowIndex, rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(bottom = 9.dp)) {
                            rowItems.forEachIndexed { colIndex, item ->
                                val active = selectedImage == item
                                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (active) Color(0xFF25294F) else Panel).clickable { selectedImage = item; activeTab = "ANALYSIS" }.padding(9.dp)) {
                                    Box(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(10.dp)).background(Brush.radialGradient(listOf(if ((rowIndex+colIndex)%2==0) Color(0xFF48518C) else Color(0xFF24546C), Color(0xFF101426)))), contentAlignment = Alignment.Center) {
                                        Text(if ((rowIndex+colIndex)%2==0) "✦" else "◉", color = Cyan, fontSize = 31.sp)
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(item, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Sample • preview only", color = Muted, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
                Text("Gallery items are illustrative placeholders; no image files are stored.", color = Muted, fontSize = 9.sp)
            }
            "ANALYSIS" -> {
                Text("ANALYSIS RESULT PREVIEW", color = Cyan, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("Visual insight workspace.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(13.dp)) {
                    Text("SELECTED IMAGE", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(selectedImage, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Brush.radialGradient(listOf(Color(0xFF4A4F89), Color(0xFF15203B), Color(0xFF080B17)))), contentAlignment = Alignment.Center) {
                        Text("✦", color = Cyan, fontSize = 50.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("DEMO RESULT", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("Sample cosmic image • bright central region • scattered light points", color = Color(0xFFD7DDF0), fontSize = 10.sp, lineHeight = 15.sp)
                    Text("Confidence: not calculated • Model: not connected", color = Muted, fontSize = 8.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RUN DEMO ANALYSIS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Violet).clickable { analysisState = "LOADING" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("SIMULATE ERROR", color = Muted, fontSize = 8.sp, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Panel2).clickable { analysisState = "ERROR" }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                if (analysisState == "LOADING") VisionStateCard("Analysis queued (demo)", "Loading state preview only. No image processing or AI request has been executed.", "LOADING")
                if (analysisState == "ERROR") VisionStateCard("Analysis unavailable", "No vision provider is connected. Try again after the analysis service is configured.", "ERROR")
                if (analysisState == "IDLE") VisionStateCard("Ready for preview", "Choose a sample image and preview the result layout.", "EMPTY")
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF14283A)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = Cyan, fontSize = 9.sp)
            Spacer(Modifier.width(8.dp))
            Text("UI preview only • camera, file picker, gallery storage and vision AI are not connected.", color = Color(0xFFB8D8E8), fontSize = 9.sp, lineHeight = 14.sp)
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
            Text(detail, color = Muted, fontSize = 9.sp, lineHeight = 14.sp)
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
                Text("GALAXY ACTIVITY • UPDATES",color=Cyan,fontSize=9.sp,letterSpacing=1.5.sp,fontWeight=FontWeight.Bold)
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
                Text("INBOX OVERVIEW",color=Cyan,fontSize=9.sp,letterSpacing=1.3.sp,fontWeight=FontWeight.Bold)
                Text("$unread unread notifications",color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text("${items.size} total • local demo inbox",color=Muted,fontSize=9.sp)
            }
            Text("$unread",color=Cyan,fontSize=25.sp,fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(if(settings) "← INBOX" else "⚙ SETTINGS",color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(Panel2).clickable { settings=!settings;selected=null }.padding(10.dp))
            Text("MARK ALL READ",color=Cyan,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(Panel).clickable { items=items.map { it.copy(unread=false) } }.padding(10.dp))
            Text("CLEAR ALL",color=Color(0xFFFF9AA9),fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(Panel).clickable { items=emptyList();selected=null }.padding(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        if(settings) {
            Text("NOTIFICATION PREFERENCES",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold)
            listOf("System updates" to system,"Research updates" to research,"Security reminders" to security,"Vision updates" to vision,"Push notifications" to push).forEachIndexed { i,p ->
                Row(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(12.dp)).background(Panel).clickable { when(i){0->system=!system;1->research=!research;2->security=!security;3->vision=!vision;4->push=!push} }.padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(p.first,color=Color.White,fontSize=10.sp,modifier=Modifier.weight(1f))
                    Text(if(p.second) "ON" else "OFF",color=if(p.second) Cyan else Muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                }
            }
            Text("Preferences are local UI state. Push delivery and Android notification permissions are not connected.",color=Muted,fontSize=9.sp,lineHeight=14.sp,modifier=Modifier.padding(top=10.dp))
        } else if(selected!=null) {
            val n=items.firstOrNull { it.id==selected }
            if(n!=null) {
                Text("NOTIFICATION DETAIL",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold)
                Column(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(15.dp)) {
                    Text(n.type,color=Cyan,fontSize=8.sp,fontWeight=FontWeight.Bold)
                    Text(n.title,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=7.dp))
                    Text(n.time,color=Muted,fontSize=9.sp)
                    Text(n.body,color=Color(0xFFD5DCEF),fontSize=11.sp,lineHeight=18.sp,modifier=Modifier.padding(top=14.dp))
                    Text(if(n.unread) "MARK AS READ" else "MARK AS UNREAD",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable { items=items.map { if(it.id==n.id) it.copy(unread=!it.unread) else it } }.padding(top=16.dp))
                    Text("DELETE NOTIFICATION",color=Color(0xFFFF9AA9),fontSize=9.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable { items=items.filterNot { it.id==n.id };selected=null }.padding(top=12.dp))
                }
            } else selected=null
        } else {
            Text("FILTER BY CATEGORY",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=7.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("ALL","SYSTEM","RESEARCH","SECURITY","VISION").forEach { f ->
                    Text(f,color=if(filter==f) Color.White else Muted,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clip(RoundedCornerShape(9.dp)).background(if(filter==f) Color(0xFF302A5B) else Panel).clickable { filter=f }.padding(horizontal=12.dp,vertical=10.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(if(filter=="ALL") "RECENT ACTIVITY" else "$filter NOTIFICATIONS",color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text(if(emptyDemo) "SHOW ITEMS" else "EMPTY PREVIEW",color=Muted,fontSize=8.sp,modifier=Modifier.clickable { emptyDemo=!emptyDemo }.padding(6.dp))
            }
            val visible=if(emptyDemo) emptyList() else items.filter { filter=="ALL" || it.type==filter }
            if(visible.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top=10.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("◇",color=Violet,fontSize=34.sp)
                    Text("You're all caught up",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold)
                    Text("No notifications to show in this view.",color=Muted,fontSize=9.sp)
                    Text("CLEAR FILTERS",color=Cyan,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.clickable { filter="ALL";emptyDemo=false }.padding(top=10.dp))
                }
            } else visible.forEach { n ->
                Row(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(14.dp)).background(if(n.unread) Color(0xFF202744) else Panel).clickable { selected=n.id;items=items.map { if(it.id==n.id) it.copy(unread=false) else it } }.padding(12.dp),verticalAlignment=Alignment.Top) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Panel2),contentAlignment=Alignment.Center) { Text(when(n.type){"RESEARCH"->"⌁";"SECURITY"->"◇";"VISION"->"◉";else->"✦"},color=Cyan,fontSize=18.sp) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text(n.title,color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            if(n.unread) Box(Modifier.size(7.dp).clip(CircleShape).background(Cyan))
                        }
                        Text(n.body,color=Muted,fontSize=9.sp,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=14.sp)
                        Text("${n.type}  •  ${n.time}",color=Color(0xFF8291B2),fontSize=8.sp,modifier=Modifier.padding(top=5.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF14283A)).padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("●",color=Cyan,fontSize=9.sp);Spacer(Modifier.width(8.dp))
            Text("Local UI demo • sample notifications, no live delivery service.",color=Color(0xFFB8D8E8),fontSize=9.sp,lineHeight=14.sp)
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
