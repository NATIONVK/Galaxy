package com.nationvk.galaxy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class GalaxyProviderConfig(
    val baseUrl: String,
    val model: String,
    val hasApiKey: Boolean
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank() && hasApiKey
}

/** Stores provider metadata locally and encrypts API credentials with Android Keystore. */
object GalaxyAiProviderStore {
    private const val PREFS = "galaxy_ai_provider"
    private const val KEY_ALIAS = "galaxy_ai_provider_key_v1"
    private const val BASE_URL = "base_url"
    private const val MODEL = "model"
    private const val ENCRYPTED_KEY = "encrypted_api_key"

    fun load(context: Context): GalaxyProviderConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GalaxyProviderConfig(
            baseUrl = prefs.getString(BASE_URL, "") ?: "",
            model = prefs.getString(MODEL, "") ?: "",
            hasApiKey = !prefs.getString(ENCRYPTED_KEY, "").isNullOrBlank()
        )
    }

    fun save(context: Context, baseUrl: String, model: String, apiKey: String): Boolean {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        val cleanModel = model.trim()
        val cleanKey = apiKey.trim()
        if (!isSafeHttpsBaseUrl(cleanUrl) || cleanModel.isBlank() || cleanModel.length > 120) return false
        return try {
            val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(BASE_URL, cleanUrl)
                .putString(MODEL, cleanModel)
            if (cleanKey.isNotEmpty()) editor.putString(ENCRYPTED_KEY, encrypt(cleanKey))
            editor.commit()
        } catch (_: Exception) {
            false
        }
    }

    fun clear(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    internal fun readApiKey(context: Context): String? {
        val encrypted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ENCRYPTED_KEY, null) ?: return null
        return try { decrypt(encrypted) } catch (_: Exception) { null }
    }

    fun isSafeHttpsBaseUrl(value: String): Boolean {
        if (value.isBlank() || value.length > 500 || value.any { it.isWhitespace() }) return false
        return try {
            val uri = java.net.URI(value)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() && uri.userInfo == null &&
                uri.query == null && uri.fragment == null
        } catch (_: Exception) { false }
    }

    private fun getSecretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > 12) { "Invalid encrypted credential" }
        val iv = combined.copyOfRange(0, 12)
        val payload = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(payload), Charsets.UTF_8)
    }
}

/** OpenAI-compatible Chat Completions transport. Network work never runs on the UI thread. */
object GalaxyAiClient {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun send(
        context: Context,
        promptContext: String,
        onResult: (Result<String>) -> Unit
    ) {
        val appContext = context.applicationContext
        executor.execute {
            val result = runCatching { performRequest(appContext, promptContext) }
            mainHandler.post { onResult(result) }
        }
    }

    private fun performRequest(context: Context, promptContext: String): String {
        val config = GalaxyAiProviderStore.load(context)
        require(config.isConfigured) { "AI provider is not configured. Open Settings → AI Provider." }
        val apiKey = GalaxyAiProviderStore.readApiKey(context)
            ?: throw IllegalStateException("Saved API key could not be decrypted. Save it again in AI Provider settings.")
        require(promptContext.isNotBlank() && promptContext.length <= 24000) { "Conversation context is empty or too large." }

        val base = config.baseUrl.trimEnd('/')
        val endpoint = if (base.endsWith("/v1", ignoreCase = true)) {
            "$base/chat/completions"
        } else {
            "$base/v1/chat/completions"
        }
        val payload = JSONObject()
            .put("model", config.model)
            .put("temperature", 0.7)
            .put("max_tokens", 1200)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", promptContext))
                .put(JSONObject().put("role", "user").put("content", "Please answer the current user request described in the system context. Reply in the user's language.")))
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(1_000_000) }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                throw IllegalStateException("Provider returned HTTP $status" + if (!detail.isNullOrBlank()) ": ${detail.take(240)}" else ". Check provider URL, model and API key.")
            }
            val root = JSONObject(body)
            val choices = root.optJSONArray("choices") ?: throw IllegalStateException("Provider response did not include choices.")
            val answer = choices.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
            if (answer.isBlank()) throw IllegalStateException("Provider returned an empty response.")
            answer.take(30000)
        } finally {
            connection.disconnect()
        }
    }
}
