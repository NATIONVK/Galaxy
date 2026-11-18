package com.nationvk.galaxy

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class GalaxyMission(val title: String, val detail: String, val glyph: String)

private val galaxyMissions = listOf(
    GalaxyMission("Learn something new", "Study or explore a useful topic", "✦"),
    GalaxyMission("Complete one focus session", "Give one task your full attention", "◉"),
    GalaxyMission("Review what you learned", "Spend a moment revising your progress", "↗")
)

/**
 * A small, offline-first daily progress tracker.
 * Existing Galaxy modules and their stored data are left untouched.
 */
@Composable
fun GalaxyDailyMissionPanel() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("galaxy_daily_missions", Context.MODE_PRIVATE)
    }
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var completed by remember(today) {
        mutableStateOf(
            prefs.getString("tasks_$today", null)
                ?.split(",")
                ?.map { it == "1" }
                ?.take(galaxyMissions.size)
                ?.let { values -> values + List(galaxyMissions.size - values.size) { false } }
                ?: List(galaxyMissions.size) { false }
        )
    }
    var streak by remember { mutableStateOf(0) }
    val doneCount = completed.count { it }
    val progress = doneCount.toFloat() / galaxyMissions.size.toFloat()

    fun persist(next: List<Boolean>) {
        completed = next
        prefs.edit().putString("tasks_$today", next.joinToString(",") { if (it) "1" else "0" }).apply()
        val history = prefs.getString("completed_days", "").orEmpty()
            .split(",").filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }.toMutableSet()
        if (next.all { it }) history.add(today) else history.remove(today)
        prefs.edit().putString("completed_days", history.sorted().joinToString(",")).apply()
        streak = calculateStreak(history)
    }

    LaunchedEffect(today) {
        val history = prefs.getString("completed_days", "").orEmpty()
            .split(",").filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }.toSet()
        streak = calculateStreak(history)
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF101326)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DAILY MISSION", color = Color(0xFF55DFFF), fontSize = 10.sp, letterSpacing = 1.8.sp)
                Text("Small steps. Bigger universe.", color = Color.White, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("✦ $streak", color = Color(0xFFFFC66D), fontSize = 17.sp)
                Text("DAY STREAK", color = Color(0xFF929AB8), fontSize = 8.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$doneCount / ${galaxyMissions.size} completed", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", color = Color(0xFF55DFFF), fontSize = 11.sp)
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
            color = Color(0xFF55DFFF),
            trackColor = Color(0xFF252B45)
        )
        Spacer(Modifier.height(10.dp))
        galaxyMissions.forEachIndexed { index, mission ->
            val checked = completed.getOrElse(index) { false }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (checked) Color(0xFF142C38) else Color(0xFF171B33))
                    .semantics { role = Role.Checkbox }
                    .clickable {
                        val next = completed.toMutableList()
                        next[index] = !next[index]
                        persist(next)
                    }.padding(horizontal = 11.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (checked) "✓" else mission.glyph, color = if (checked) Color(0xFF55DFFF) else Color(0xFF9B69FF), fontSize = 16.sp)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(mission.title, color = if (checked) Color(0xFF9EABC4) else Color.White, fontSize = 11.sp)
                    Text(mission.detail, color = Color(0xFF929AB8), fontSize = 9.sp)
                }
                Text(if (checked) "DONE" else "MARK", color = if (checked) Color(0xFF55DFFF) else Color(0xFF929AB8), fontSize = 8.sp)
            }
            if (index != galaxyMissions.lastIndex) Spacer(Modifier.height(7.dp))
        }
        if (doneCount == galaxyMissions.size) {
            Spacer(Modifier.height(10.dp))
            Text("Mission complete. You showed up for yourself today. ✨", color = Color(0xFF7DE8C5), fontSize = 10.sp)
        }
        Text("Saved privately on this device • resets for a new day", color = Color(0xFF68718F), fontSize = 8.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

private fun calculateStreak(history: Set<String>): Int {
    var day = LocalDate.now()
    var count = 0
    while (history.contains(day.format(DateTimeFormatter.ISO_LOCAL_DATE))) {
        count++
        day = day.minusDays(1)
    }
    return count
}
