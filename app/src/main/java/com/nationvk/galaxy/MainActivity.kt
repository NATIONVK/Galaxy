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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    HOME("Home", "⌂"), CHAT("AI Chat", "✧"), LAB("Lab", "⌘"), MEMORY("Memory", "◈"), PROFILE("Profile", "◎")
}

@Composable
fun GalaxyApp() {
    var selected by remember { mutableStateOf(GalaxyTab.HOME) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Space) {
            Box(Modifier.fillMaxSize()) {
                StarBackdrop(Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize()) {
                    GalaxyTopBar()
                    Box(Modifier.weight(1f)) {
                        when (selected) {
                            GalaxyTab.HOME -> HomeScreen(onOpen = { selected = it })
                            GalaxyTab.CHAT -> FeatureScreen("AI CHAT", "তোমার ব্যক্তিগত AI কথোপকথন", "A thoughtful assistant, ready when you are.", "Chat interface is being prepared. AI connection will be added in the core phase.", "Start a conversation", "Conversation engine coming next")
                            GalaxyTab.LAB -> FeatureScreen("CYBER LAB", "নিরাপদে শেখো, পরীক্ষা করো", "Your personal learning & security workspace.", "Practice area, learning paths and local sandbox tools will be built here.", "Explore learning paths", "Lab tools are not connected yet")
                            GalaxyTab.MEMORY -> FeatureScreen("MEMORY VAULT", "তোমার জ্ঞান, এক জায়গায়", "A private space for saved context and notes.", "Memory controls, searchable notes and personal preferences will be added in a later phase.", "Open memory vault", "Memory storage is not active yet")
                            GalaxyTab.PROFILE -> ProfileScreen()
                        }
                    }
                    BottomNavigation(selected = selected, onSelect = { selected = it })
                }
            }
        }
    }
}

@Composable
private fun GalaxyTopBar() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Violet, Color(0xFF242A58)))),
            contentAlignment = Alignment.Center
        ) { Text("✦", color = Color.White, fontSize = 23.sp) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("GALAXY", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text("PERSONAL AI UNIVERSE", color = Cyan, fontSize = 8.sp, letterSpacing = 2.sp)
        }
        Box(
            Modifier.clip(CircleShape).background(Color(0xFF14283A)).padding(horizontal = 11.dp, vertical = 7.dp)
        ) { Text("●  SYSTEM DESIGN", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun HomeScreen(onOpen: (GalaxyTab) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WELCOME TO YOUR UNIVERSE", color = Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
                Text("Hello, Explorer.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.clip(CircleShape).background(Panel2).padding(11.dp)) {
                Text("✧", color = Violet, fontSize = 21.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(25.dp))) {
            GalaxyGLSurfaceViewHost(Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD050611))))
                    .padding(18.dp)
            ) {
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
            ModuleCard("⌁", "RESEARCH", "Analysis workspace", Modifier.weight(1f)) { onOpen(GalaxyTab.LAB) }
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
    Column(
        modifier.clip(RoundedCornerShape(19.dp))
            .background(Brush.linearGradient(listOf(Panel2, Panel)))
            .clickable(onClick = onClick).padding(15.dp)
    ) {
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
private fun FeatureScreen(tag: String, title: String, subtitle: String, detail: String, action: String, status: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(18.dp))
        Text(tag, color = Cyan, fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF34306C), Panel, Space)))
        , contentAlignment = Alignment.Center) {
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
private fun ProfileScreen() {
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
    }
}

@Composable
private fun BottomNavigation(selected: GalaxyTab, onSelect: (GalaxyTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF090B17)).padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GalaxyTab.values().forEach { tab ->
            val active = tab == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(tab) }.padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
        val stars = listOf(
            0.06f to 0.12f, 0.91f to 0.10f, 0.18f to 0.31f, 0.86f to 0.42f,
            0.08f to 0.66f, 0.94f to 0.75f, 0.63f to 0.22f, 0.42f to 0.86f,
            0.72f to 0.92f, 0.30f to 0.52f, 0.52f to 0.08f
        )
        stars.forEachIndexed { index, p ->
            drawCircle(if (index % 3 == 0) Cyan.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.55f),
                if (index % 4 == 0) 2.0f else 1.1f, androidx.compose.ui.geometry.Offset(p.first * size.width, p.second * size.height))
        }
    }
}
