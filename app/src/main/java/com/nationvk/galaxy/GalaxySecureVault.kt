package com.nationvk.galaxy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private object GalaxyVaultCrypto {
    private const val ITERATIONS = 310_000
    private const val AAD = "GALAXY-SECURE-VAULT-v1"

    fun encrypt(plain: String, password: CharArray): String {
        require(password.size >= 12) { "Master Password কমপক্ষে ১২ অক্ষরের হতে হবে।" }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("format", "GALAXY_API_VAULT")
            .put("version", 1)
            .put("kdf", "PBKDF2-HMAC-SHA256")
            .put("iterations", ITERATIONS)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()
    }

    fun decrypt(envelope: String, password: CharArray): String {
        val o = JSONObject(envelope)
        require(o.optString("format") == "GALAXY_API_VAULT" && o.optInt("version") == 1) {
            "Vault format/version supported নয়।"
        }
        require(o.optString("kdf") == "PBKDF2-HMAC-SHA256") { "Unknown key derivation method." }
        val rounds = o.optInt("iterations")
        require(rounds in 310_000..2_000_000) { "Invalid KDF settings." }
        val salt = Base64.decode(o.getString("salt"), Base64.DEFAULT)
        val iv = Base64.decode(o.getString("iv"), Base64.DEFAULT)
        val encrypted = Base64.decode(o.getString("ciphertext"), Base64.DEFAULT)
        require(salt.size == 16 && iv.size == 12 && encrypted.size >= 16) { "Vault data damaged." }
        val key = derive(password, salt, rounds)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun derive(password: CharArray, salt: ByteArray, rounds: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, rounds, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}

private object GalaxyVaultTokenStore {
    private const val PREF = "galaxy_secure_vault"
    private const val TOKEN = "github_token"
    private const val ALIAS = "galaxy_vault_github_token_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun save(context: Context, token: String) {
        require(token.startsWith("github_pat_") || token.startsWith("ghp_")) {
            "GitHub token format সঠিক মনে হচ্ছে না।"
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val packed = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        check(context.getSharedPreferences(PREF, 0).edit()
            .putString(TOKEN, Base64.encodeToString(packed, Base64.NO_WRAP)).commit()) {
            "Token save করা যায়নি।"
        }
    }

    fun load(context: Context): String? {
        val packed = context.getSharedPreferences(PREF, 0).getString(TOKEN, null) ?: return null
        val bytes = Base64.decode(packed, Base64.NO_WRAP)
        require(bytes.size > 28) { "Saved GitHub token corrupted." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, 0).edit().remove(TOKEN).apply()
    }
}

private object GalaxyVaultGitHub {
    private const val OWNER = "NATIONVK"
    private const val REPO = "Galaxy-Secure-Vault-Private"
    private const val PATH = "galaxy-api-vault.json"
    private const val API = "https://api.github.com"

    fun upload(token: String, encryptedEnvelope: String): String {
        verifyPrivateRepo(token)
        val existingSha = getFile(token)?.optString("sha").orEmpty()
        val payload = JSONObject()
            .put("message", "Update encrypted Galaxy API vault")
            .put("content", Base64.encodeToString(encryptedEnvelope.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        if (existingSha.isNotBlank()) payload.put("sha", existingSha)
        val response = request(token, "PUT", "$API/repos/$OWNER/$REPO/contents/$PATH", payload.toString())
        return "Encrypted backup GitHub-এ sync হয়েছে। Commit: " + response.optJSONObject("commit")?.optString("sha").orEmpty().take(10)
    }

    fun download(token: String): String {
        verifyPrivateRepo(token)
        val file = getFile(token) ?: throw IllegalStateException("এখনো কোনো encrypted backup নেই।")
        val encoded = file.optString("content").replace("\n", "")
        require(encoded.isNotBlank()) { "Backup content পাওয়া যায়নি।" }
        return String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
    }

    private fun verifyPrivateRepo(token: String) {
        val repo = request(token, "GET", "$API/repos/$OWNER/$REPO")
        require(repo.optBoolean("private", false)) { "Repository Private নয়—sync বন্ধ রাখা হয়েছে।" }
    }

    private fun getFile(token: String): JSONObject? {
        val url = "$API/repos/$OWNER/$REPO/contents/$PATH"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 25_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        return try {
            val code = connection.responseCode
            if (code == 404) null
            else {
                val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(1_000_000) }.orEmpty()
                if (code !in 200..299) throw IllegalStateException("GitHub HTTP $code: " + errorMessage(raw))
                JSONObject(raw)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun request(token: String, method: String, url: String, body: String? = null): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(1_000_000) }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("GitHub HTTP $code: " + errorMessage(raw))
            JSONObject(raw)
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(raw: String): String =
        runCatching { JSONObject(raw).optString("message").take(180) }.getOrDefault("Request failed")
}

@Composable
fun GalaxySecureVaultSettingsPanel() {
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var master by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("GitHub token ও Master Password সেট করে encrypted backup sync করো।") }
    var busy by remember { mutableStateOf(false) }
    var tokenSaved by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val card = Color(0xFF101326)
    val field = Color(0xFF171B33)
    val cyan = Color(0xFF55DFFF)
    val muted = Color(0xFF929AB8)
    val violet = Color(0xFF9B69FF)

    fun runVault(action: String) {
        if (busy) return
        val password = master.toCharArray()
        if (password.size < 12) {
            status = "Master Password কমপক্ষে ১২ অক্ষরের দাও।"
            password.fill('\u0000')
            return
        }
        busy = true
        status = if (action == "upload") "Provider profile encrypt করে GitHub-এ পাঠানো হচ্ছে…" else "Encrypted backup ডাউনলোড ও decrypt করা হচ্ছে…"
        executor.execute {
            val result = runCatching {
                val githubToken = GalaxyVaultTokenStore.load(context)
                    ?: throw IllegalStateException("আগে GitHub token Save করো।")
                if (action == "upload") {
                    val plain = GalaxyApiProfiles.exportForVault(context)
                    val envelope = GalaxyVaultCrypto.encrypt(plain, password)
                    GalaxyVaultGitHub.upload(githubToken, envelope)
                } else {
                    val envelope = GalaxyVaultGitHub.download(githubToken)
                    val plain = GalaxyVaultCrypto.decrypt(envelope, password)
                    GalaxyApiProfiles.importFromVault(context, plain)
                    "Backup restore হয়েছে। Provider list Settings refresh করে দেখো।"
                }
            }
            password.fill('\u0000')
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                busy = false
                master = ""
                status = result.fold({ it }, { "ব্যর্থ: " + (it.message ?: "Unknown error") })
            }
        }
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(card).padding(14.dp)) {
        Text("SECURE API VAULT • ENCRYPTED CLOUD BACKUP", color = cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            "API profile ও secret AES-256-GCM দিয়ে encrypt করে শুধু তোমার Private GitHub repository-তে রাখা হবে। Master Password কোথাও save হয় না।",
            color = muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )
        @Composable
        fun input(label: String, value: String, onChange: (String) -> Unit, secret: Boolean = false) {
            Text(label, color = muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 11.sp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(field).padding(11.dp),
                decorationBox = { inner -> Box { if (value.isEmpty()) Text(if (secret) "••••••••••••" else label, color = muted, fontSize = 11.sp); inner() } }
            )
        }
        input("GITHUB FINE-GRAINED TOKEN", token, { token = it.take(300) }, true)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SAVE TOKEN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(violet).clickable {
                    runCatching { GalaxyVaultTokenStore.save(context, token.trim()); token = ""; tokenSaved = true; status = "GitHub token Android Keystore-এ encrypted হয়েছে।" }
                        .onFailure { status = it.message ?: "Token save failed." }
                }.padding(12.dp))
            Text("REMOVE TOKEN", color = Color(0xFFFF9AAB), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(field).clickable {
                    GalaxyVaultTokenStore.clear(context); tokenSaved = false; status = "Saved token সরানো হয়েছে।"
                }.padding(12.dp))
        }
        Text(if (tokenSaved) "Token saved this device • encrypted" else "Token input একবার save করলে পরে আবার দিতে হবে না।", color = muted, fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp))
        input("MASTER PASSWORD (১২+ CHARACTERS)", master, { master = it.take(128) }, true)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (busy) "WORKING…" else "UPLOAD ENCRYPTED BACKUP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(violet).clickable(enabled = !busy) { runVault("upload") }.padding(12.dp))
            Text(if (busy) "WAIT…" else "RESTORE BACKUP", color = cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(field).clickable(enabled = !busy) { runVault("restore") }.padding(12.dp))
        }
        Text(status, color = muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 10.dp))
        Text(
            "Token scope: শুধু এই repository-তে Contents Read/Write; Metadata Read-only। Master Password ভুলে গেলে backup restore করা যাবে না। API Key কখনো Chat বা GitHub source code-এ paste করো না।",
            color = muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 8.dp)
        )
    }
}
