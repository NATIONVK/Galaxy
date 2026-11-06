package com.nationvk.galaxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GalaxyHome() }
    }
}

private val Space = Color(0xFF03040B)
private val Violet = Color(0xFF9B5CFF)
private val Cyan = Color(0xFF42D9FF)

@Composable
fun GalaxyHome() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Space) {
            Box(Modifier.fillMaxSize()) {
                StarField(Modifier.fillMaxSize())
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GALAXY", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 7.sp)
                    Text("YOUR PERSONAL AI UNIVERSE", color = Cyan, fontSize = 10.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(18.dp))
                    BlackHole(Modifier.fillMaxWidth().height(330.dp))
                    Text("The universe is listening.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("একটি নতুন AI অভিজ্ঞতার শুরু", color = Color(0xFF9BA6C8), fontSize = 13.sp)
                    Spacer(Modifier.height(28.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureTile("VOICE", "কথা বলো", Modifier.weight(1f))
                        FeatureTile("MEMORY", "মনে রাখবে", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureTile("RESEARCH", "বিশ্লেষণ", Modifier.weight(1f))
                        FeatureTile("CYBER LAB", "ল্যাব", Modifier.weight(1f))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("NATIONVK  •  PROJECT GALAXY AI  •  v0.1.0", color = Color(0xFF59627D), fontSize = 9.sp, letterSpacing = 1.2.sp)
                }
            }
        }
    }
}

@Composable
private fun BlackHole(modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "black-hole-motion")
    val rotation by motion.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "disk-rotation"
    )
    val pulse by motion.animateFloat(
        initialValue = 0.88f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "glow-pulse"
    )
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.235f
        drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = 0.30f * pulse), Color.Transparent), c, radius * 2.6f), radius * 2.6f, c)
        rotate(rotation, c) {
            drawOval(
                brush = Brush.sweepGradient(listOf(Color.Transparent, Violet, Cyan, Color.White, Violet, Color.Transparent), c),
                topLeft = Offset(c.x - radius * 2.25f, c.y - radius * 0.55f),
                size = Size(radius * 4.5f, radius * 1.1f),
                style = Stroke(width = radius * 0.20f)
            )
            drawOval(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, Violet.copy(alpha = 0.85f), Color.White, Cyan, Color.Transparent)),
                topLeft = Offset(c.x - radius * 1.85f, c.y - radius * 0.22f),
                size = Size(radius * 3.7f, radius * 0.44f),
                style = Stroke(width = radius * 0.08f)
            )
        }
        drawCircle(Color(0xFF010105), radius, c)
        drawCircle(Brush.radialGradient(listOf(Color.Black, Color(0xFF05030C), Color.Transparent), c, radius * 1.18f), radius * 1.18f, c, style = Stroke(width = radius * 0.10f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.rotate(degrees: Float, pivot: Offset, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    androidx.compose.ui.graphics.drawscope.rotate(degrees, pivot, block)
}

@Composable
private fun StarField(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val points = listOf(
            Offset(.08f * size.width, .12f * size.height), Offset(.88f * size.width, .16f * size.height),
            Offset(.17f * size.width, .42f * size.height), Offset(.91f * size.width, .52f * size.height),
            Offset(.10f * size.width, .78f * size.height), Offset(.82f * size.width, .88f * size.height),
            Offset(.56f * size.width, .08f * size.height), Offset(.72f * size.width, .68f * size.height)
        )
        points.forEachIndexed { i, p -> drawCircle(if (i % 2 == 0) Cyan.copy(alpha = .65f) else Color.White.copy(alpha = .7f), if (i % 3 == 0) 2.2f else 1.2f, p) }
    }
}

@Composable
private fun FeatureTile(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF17172B), Color(0xFF0A1020))))
            .padding(16.dp)
    ) {
        Text(title, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
