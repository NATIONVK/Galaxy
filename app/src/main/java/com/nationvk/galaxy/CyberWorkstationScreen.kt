package com.nationvk.galaxy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.InetAddress
import java.util.Locale

private val WBg = Color(0xFF050611)
private val WPanel = Color(0xFF101326)
private val WPanel2 = Color(0xFF171B33)
private val WCyan = Color(0xFF55DFFF)
private val WViolet = Color(0xFF9B69FF)
private val WMuted = Color(0xFF929AB8)

private enum class WorkArea(val title: String, val detail: String, val glyph: String) {
    OVERVIEW("Overview", "Command center", "⌂"),
    TERMINAL("Termux Studio", "Shell workspace", ">_"),
    CODE("Code Workspace", "Local code drafts", "{ }"),
    GITHUB("GitHub Center", "Repositories & Actions", "⌘"),
    NETWORK("Network Diagnostics", "DNS lookup", "⌁"),
    APK("APK Forge", "Android build automation", "▣"),
    FILES("Project Files", "Android file picker", "▤")
}

@Composable
internal fun CyberWorkstationScreen() {
    val context = LocalContext.current
    var area by remember { mutableStateOf(WorkArea.OVERVIEW) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NATIONVK • GALAXY", color = WCyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Cyber Workstation", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("ANDROID DEVELOPMENT COMMAND CENTER", color = WMuted, fontSize = 9.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(WCyan.copy(alpha=.3f), WViolet.copy(alpha=.4f)))), contentAlignment = Alignment.Center) {
                Text("⌘", color = WCyan, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF1B3652), WPanel))).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WORKSTATION STATUS", color = WCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("Ready for mobile development", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Termux • GitHub • Code • Network • APK", color = WMuted, fontSize = 9.sp)
            }
            Text("LOCAL", color = Color(0xFF8BE0B5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        WorkArea.entries.chunked(2).forEach { items ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(if (area == item) Color(0xFF25294F) else WPanel)
                        .clickable { area = item }.padding(11.dp)) {
                        Text(item.glyph, color = WCyan, fontSize = 18.sp)
                        Text(item.title.uppercase(Locale.ROOT), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(item.detail, color = WMuted, fontSize = 8.sp)
                    }
                }
                if (items.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(7.dp))
        }
        Spacer(Modifier.height(5.dp))
        when (area) {
            WorkArea.OVERVIEW -> WorkOverview(context) { area = it }
            WorkArea.TERMINAL -> TermuxPanel(context)
            WorkArea.CODE -> CodePanel(context)
            WorkArea.GITHUB -> GitHubPanel(context)
            WorkArea.NETWORK -> NetworkPanel()
            WorkArea.APK -> ApkPanel(context)
            WorkArea.FILES -> FilesPanel(context)
        }
        Spacer(Modifier.height(16.dp))
        Text("VOICE ASSISTANT REMAINS SEPARATE • BLACK HOLE UI AND EXISTING SETTINGS PRESERVED", color = WMuted, fontSize = 8.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun WTitle(title: String, subtitle: String) {
    Text(title, color = WCyan, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = WMuted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top=4.dp, bottom=9.dp))
}

@Composable
private fun WAction(title: String, detail: String, action: String = "OPEN", onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom=7.dp).clip(RoundedCornerShape(12.dp)).background(WPanel).clickable(onClick=onClick).padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color=Color.White, fontSize=11.sp, fontWeight=FontWeight.SemiBold)
            Text(detail, color=WMuted, fontSize=9.sp, lineHeight=13.sp)
        }
        Text(action, color=WCyan, fontSize=9.sp, fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun WButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Text(label, color=Color.White, fontSize=9.sp, fontWeight=FontWeight.Bold,
        modifier=modifier.clip(RoundedCornerShape(9.dp)).background(if(enabled) WViolet else WPanel2)
            .clickable(enabled=enabled, onClick=onClick).padding(horizontal=12.dp, vertical=12.dp))
}

@Composable
private fun WInput(label: String, value: String, onChange: (String)->Unit, hint: String) {
    Text(label, color=WMuted, fontSize=9.sp, fontWeight=FontWeight.Bold)
    BasicTextField(value=value, onValueChange=onChange, singleLine=true,
        modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(WPanel2).padding(11.dp),
        textStyle=TextStyle(color=Color.White, fontSize=11.sp),
        decorationBox={ inner -> Box { if(value.isEmpty()) Text(hint, color=WMuted, fontSize=11.sp); inner() } })
}

@Composable
private fun WNotice(message: String) {
    Text(message, color=WMuted, fontSize=9.sp, lineHeight=14.sp,
        modifier=Modifier.padding(top=9.dp).clip(RoundedCornerShape(10.dp)).background(WPanel2).fillMaxWidth().padding(11.dp))
}

@Composable
private fun WorkOverview(context: Context, open: (WorkArea)->Unit) {
    WTitle("WORKSPACE OVERVIEW", "Your Android-first development launchpad.")
    WAction("Device profile", "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}", "INFO")
    WAction("Termux Studio", "Open the installed terminal for real shell execution.") { open(WorkArea.TERMINAL) }
    WAction("Code Workspace", "Write and save code drafts locally.") { open(WorkArea.CODE) }
    WAction("GitHub Center", "Repository, Actions, pull requests and releases.") { open(WorkArea.GITHUB) }
    WAction("Network Diagnostics", "User-requested DNS lookup.") { open(WorkArea.NETWORK) }
    WAction("APK Forge", "Build debug APKs with GitHub Actions.") { open(WorkArea.APK) }
    WAction("Project Files", "Open files using Android's document picker.") { open(WorkArea.FILES) }
}

@Composable
private fun TermuxPanel(context: Context) {
    var status by remember { mutableStateOf("Termux launch not requested.") }
    WTitle("TERMUX STUDIO", "Shell actions are handed off to Termux; Galaxy does not execute commands invisibly.")
    WAction("Launch Termux", "Open installed Termux app.", "LAUNCH") {
        try {
            val launch=context.packageManager.getLaunchIntentForPackage("com.termux")
            if(launch!=null) { context.startActivity(launch); status="Termux launch requested." }
            else { openWUrl(context,"https://termux.dev/"); status="Termux not installed; opened official site." }
        } catch(e:Exception) { status="Launch failed: ${e.message ?: "unknown error"}" }
    }
    WAction("Copy developer setup", "pkg update && pkg upgrade && pkg install git python clang", "COPY") {
        copyW(context,"pkg update && pkg upgrade && pkg install git python clang")
        status="Commands copied. Review before running."
    }
    WAction("Termux GitHub", "Official Termux application repository.", "OPEN") { openWUrl(context,"https://github.com/termux/termux-app") }
    Text(status,color=WMuted,fontSize=9.sp)
    WNotice("Review commands before executing them. Broad shell permissions are not requested.")
}

@Composable
private fun CodePanel(context: Context) {
    val prefs= remember { context.getSharedPreferences("galaxy_workstation",Context.MODE_PRIVATE) }
    var name by remember { mutableStateOf(prefs.getString("code_name","main.py") ?: "main.py") }
    var source by remember { mutableStateOf(prefs.getString("code_draft","# NATIONVK / Galaxy\nprint(\"Hello, Developer!\")") ?: "") }
    var status by remember { mutableStateOf("Drafts are stored locally on this device.") }
    WTitle("CODE WORKSPACE","A lightweight scratchpad. Execute reviewed code in Termux or a dedicated IDE.")
    WInput("FILE NAME",name,{name=it.take(80)},"main.py")
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF080B18)).padding(11.dp)) {
        BasicTextField(value=source,onValueChange={source=it.take(30000)},modifier=Modifier.fillMaxWidth().heightIn(min=210.dp),
            textStyle=TextStyle(color=Color(0xFFD6E8FF),fontSize=11.sp,lineHeight=17.sp),
            decorationBox={inner->Box { if(source.isEmpty()) Text("Write code here…",color=WMuted); inner() }})
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        WButton("SAVE DRAFT",Modifier.weight(1f)) { prefs.edit().putString("code_name",name).putString("code_draft",source).apply(); status="Draft saved locally." }
        WButton("COPY CODE",Modifier.weight(1f)) { copyW(context,source); status="Code copied to clipboard." }
    }
    Spacer(Modifier.height(8.dp))
    WButton("SHARE CODE",Modifier.fillMaxWidth()) {
        try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,name).putExtra(Intent.EXTRA_TEXT,source),"Share code")); status="Share sheet opened." }
        catch(e:Exception) { status="Share failed: ${e.message ?: "unknown error"}" }
    }
    Text(status,color=WMuted,fontSize=9.sp,modifier=Modifier.padding(top=8.dp))
    WNotice("This editor saves drafts only. It does not claim to compile or execute code.")
}

@Composable
private fun GitHubPanel(context: Context) {
    WTitle("GITHUB CENTER","Shortcuts for the existing NATIONVK/Galaxy repository.")
    WAction("Galaxy repository","Browse source code.") { openWUrl(context,"https://github.com/NATIONVK/Galaxy") }
    WAction("Android build workflow","Inspect APK workflow runs and artifacts.") { openWUrl(context,"https://github.com/NATIONVK/Galaxy/actions/workflows/android-apk.yml") }
    WAction("Pull requests","Review proposed changes before merging.") { openWUrl(context,"https://github.com/NATIONVK/Galaxy/pulls") }
    WAction("Releases","Find published packages.") { openWUrl(context,"https://github.com/NATIONVK/Galaxy/releases") }
    WNotice("Authenticated GitHub actions remain on GitHub. No personal access token is stored in this app.")
}

@Composable
private fun NetworkPanel() {
    var host by remember { mutableStateOf("example.com") }
    var status by remember { mutableStateOf("Enter a domain and tap DNS LOOKUP.") }
    var busy by remember { mutableStateOf(false) }
    WTitle("NETWORK DIAGNOSTICS","A basic DNS lookup for the domain you enter.")
    WInput("DOMAIN",host,{host=it.trim().take(253)},"example.com")
    Spacer(Modifier.height(8.dp))
    WButton(if(busy) "LOOKING UP…" else "DNS LOOKUP",Modifier.fillMaxWidth(),!busy) {
        val target=host.trim().removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':')
        if(target.isBlank() || target.any { !(it.isLetterOrDigit() || it=='.' || it=='-') }) status="Enter a valid domain."
        else {
            busy=true; status="Resolving $target…"
            Thread {
                val result=try { InetAddress.getAllByName(target).mapNotNull { it.hostAddress }.distinct().joinToString("\n") }
                    catch(e:Exception) { "DNS lookup failed: ${e.message ?: "unknown error"}" }
                Handler(Looper.getMainLooper()).post { status=result; busy=false }
            }.start()
        }
    }
    Text(status,color=Color(0xFFB8D8E8),fontSize=10.sp,lineHeight=15.sp,modifier=Modifier.padding(top=9.dp).clip(RoundedCornerShape(10.dp)).background(WPanel).fillMaxWidth().padding(12.dp))
    WNotice("This is not a port or vulnerability scanner. It performs a normal DNS lookup only.")
}

@Composable
private fun ApkPanel(context: Context) {
    WTitle("APK FORGE","Use GitHub Actions to build a reproducible Android debug APK.")
    WAction("Open Android APK workflow","Trigger or inspect the build workflow.") { openWUrl(context,"https://github.com/NATIONVK/Galaxy/actions/workflows/android-apk.yml") }
    WAction("Latest workflow runs","Check status and download artifacts.") { openWUrl(context,"https://github.com/NATIONVK/Galaxy/actions") }
    WAction("Release page","Check published release packages.") { openWUrl(context,"https://github.com/NATIONVK/Galaxy/releases") }
    WNotice("A successful workflow creates a debug APK artifact. This panel does not claim a build has already run.")
}

@Composable
private fun FilesPanel(context: Context) {
    var status by remember { mutableStateOf("Choose a file action.") }
    WTitle("PROJECT FILES","Android's system picker controls access. Galaxy only sees files you select.")
    WAction("Open a project file","Choose a file with Android's document picker.","BROWSE") {
        try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),"Open project file")); status="Document picker opened." }
        catch(e:Exception) { status="Could not open picker: ${e.message ?: "unknown error"}" }
    }
    WAction("Open Downloads","Launch Android file browser.","OPEN") {
        try { context.startActivity(Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)); status="File picker opened." }
        catch(e:Exception) { status="Could not open files: ${e.message ?: "unknown error"}" }
    }
    Text(status,color=WMuted,fontSize=9.sp)
    WNotice("Galaxy does not scan, modify or upload your files without your selection.")
}

private fun copyW(context:Context,text:String) {
    val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Galaxy Workstation",text))
}
private fun openWUrl(context:Context,url:String) {
    context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
}
