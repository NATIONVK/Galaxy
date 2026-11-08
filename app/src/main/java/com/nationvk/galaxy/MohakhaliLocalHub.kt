package com.nationvk.galaxy

import android.content.Intent
import android.net.Uri
import android.util.Xml
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LocalBg = Color(0xFF050611)
private val LocalPanel = Color(0xFF101326)
private val LocalPanel2 = Color(0xFF171B33)
private val LocalCyan = Color(0xFF55DFFF)
private val LocalMuted = Color(0xFF929AB8)

private data class LocalFeedItem(val title: String, val link: String, val source: String, val date: String)
private data class LocalFeedCategory(val label: String, val query: String, val description: String)

private val localCategories = listOf(
    LocalFeedCategory("ALL NEWS", "Mohakhali OR মহাখালী Dhaka", "মহাখালী ও আশপাশের সাম্প্রতিক খবর"),
    LocalFeedCategory("TRAFFIC", "Mohakhali traffic OR যানজট Dhaka", "যানজট ও যাতায়াত নিয়ে প্রকাশিত খবর"),
    LocalFeedCategory("WEATHER", "Dhaka weather rain forecast", "আবহাওয়া ও বৃষ্টির সংবাদ"),
    LocalFeedCategory("ALERTS", "Mohakhali Dhaka accident OR fire OR waterlogging", "দুর্ঘটনা, আগুন ও জরুরি ঘটনার খবর"),
    LocalFeedCategory("CAMPUS", "Mohakhali Dhaka campus university event education", "ক্যাম্পাস, শিক্ষা ও স্থানীয় আয়োজন"),
    LocalFeedCategory("JOBS", "Mohakhali Dhaka jobs tuition hiring", "চাকরি, নিয়োগ ও টিউশন-সম্পর্কিত খবর"),
    LocalFeedCategory("LOCAL", "Mohakhali Dhaka hospital service market business", "হাসপাতাল, সেবা ও স্থানীয় ব্যবসা")
)

private suspend fun loadLocalFeed(query: String): List<LocalFeedItem> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val endpoint = "https://news.google.com/rss/search?q=" + encoded + "&hl=bn&gl=BD&ceid=BD:bn"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    connection.setRequestProperty("User-Agent", "GalaxyAI/0.1 Android")
    try {
        connection.inputStream.use { input ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, "UTF-8")
            val result = mutableListOf<LocalFeedItem>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && result.size < 25) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    var title = ""
                    var link = ""
                    var source = ""
                    var date = ""
                    var inItem = true
                    while (inItem) {
                        when (parser.next()) {
                            XmlPullParser.START_TAG -> when (parser.name) {
                                "title" -> title = parser.nextText()
                                "link" -> link = parser.nextText()
                                "source" -> source = parser.nextText()
                                "pubDate" -> date = parser.nextText()
                            }
                            XmlPullParser.END_TAG -> if (parser.name == "item") inItem = false
                            XmlPullParser.END_DOCUMENT -> inItem = false
                        }
                    }
                    if (title.isNotBlank() && link.startsWith("https://")) {
                        result.add(LocalFeedItem(title.trim(), link.trim(), source.trim(), date.trim()))
                    }
                }
                event = parser.next()
            }
            result.distinctBy { it.link }
        }
    } finally {
        connection.disconnect()
    }
}

@Composable
fun MohakhaliLocalHubScreen() {
    val context = LocalContext.current
    var categoryIndex by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var articles by remember { mutableStateOf(emptyList<LocalFeedItem>()) }
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("সাম্প্রতিক খবর লোড হচ্ছে…") }
    var updatedAt by remember { mutableStateOf("") }
    val category = localCategories[categoryIndex]

    LaunchedEffect(categoryIndex, refreshKey) {
        isLoading = true
        status = "Mohakhali ও Dhaka-সংক্রান্ত প্রকাশিত খবর খোঁজা হচ্ছে…"
        try {
            articles = loadLocalFeed(category.query)
            status = if (articles.isEmpty()) "এই বিভাগে কোনো ফলাফল পাওয়া যায়নি।" else articles.size.toString() + "টি ফলাফল পাওয়া গেছে"
            updatedAt = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "BD")).format(Date())
        } catch (_: Exception) {
            articles = emptyList()
            status = "খবর লোড হয়নি। Internet connection পরীক্ষা করে Refresh করো।"
        } finally {
            isLoading = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("MOHAKHALI LOCAL HUB", color = LocalCyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text("Your area, one feed.", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("মহাখালী, ঢাকা • Local news & community updates", color = LocalMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(LocalPanel).clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/@23.7808,90.4143,14z/data=!5m1!1e1"))) }
            }.padding(12.dp)) {
                Text("TRAFFIC MAP", color = LocalCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("Google Maps live traffic", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(LocalPanel).clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=Dhaka+weather"))) }
            }.padding(12.dp)) {
                Text("WEATHER", color = LocalCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("Current Dhaka weather", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            localCategories.forEachIndexed { index, item ->
                val active = categoryIndex == index
                Text(item.label, color = if (active) LocalBg else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(if (active) LocalCyan else LocalPanel2)
                        .clickable { categoryIndex = index }.padding(horizontal = 12.dp, vertical = 9.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LocalPanel).padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(category.label, color = LocalCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text(category.description, color = LocalMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Text(if (isLoading) "LOADING…" else "↻ REFRESH", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(LocalPanel2).clickable { if (!isLoading) refreshKey++ }.padding(horizontal = 10.dp, vertical = 9.dp))
            }
            Text(status, color = LocalMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
            if (updatedAt.isNotBlank()) Text("Updated: " + updatedAt, color = LocalMuted, fontSize = 8.sp)
            Spacer(Modifier.height(8.dp))
            if (isLoading && articles.isEmpty()) {
                Text("Loading local updates…", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(vertical = 16.dp))
            } else if (articles.isEmpty()) {
                Text("কোনো ফলাফল নেই। Internet চালু রেখে আবার চেষ্টা করো।", color = LocalMuted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                articles.forEach { article ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(11.dp)).background(LocalPanel2)
                        .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.link))) } }
                        .padding(11.dp)) {
                        Text(article.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
                        Text(listOf(article.source.ifBlank { "News source" }, article.date).filter { it.isNotBlank() }.joinToString(" • "),
                            color = LocalCyan, fontSize = 8.sp, modifier = Modifier.padding(top = 6.dp))
                        Text("OPEN SOURCE ↗", color = LocalMuted, fontSize = 8.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Source: Google News RSS search results. Articles open at original publishers. Results may include wider Dhaka stories, not only Mohakhali. Traffic and weather buttons open external services; this screen does not independently verify live road conditions or emergency reports.",
            color = LocalMuted, fontSize = 8.sp, lineHeight = 12.sp, modifier = Modifier.padding(bottom = 14.dp))
    }
}
