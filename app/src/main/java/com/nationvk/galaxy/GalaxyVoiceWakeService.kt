package com.nationvk.galaxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class GalaxyVoiceWakeService : Service() {
    companion object {
        const val ACTION_STOP = "com.nationvk.galaxy.voice.STOP"
        const val ACTION_TOGGLE = "com.nationvk.galaxy.voice.TOGGLE"
        const val ACTION_CANCEL_PENDING = "com.nationvk.galaxy.voice.CANCEL_PENDING"
        const val ACTION_CONFIRM_MEMORY_DELETE = "com.nationvk.galaxy.voice.CONFIRM_MEMORY_DELETE"
        const val ACTION_CANCEL_MEMORY_DELETE = "com.nationvk.galaxy.voice.CANCEL_MEMORY_DELETE"
        private const val PREF_PENDING_DELETE = "pending_memory_delete_id"
        private const val CHANNEL_ID = "galaxy_voice_assistant"
        private const val NOTIFICATION_ID = 4107
    }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private var stopped = false
    private var restartQueued = false
    private var wakeArmed = false
    private var tts: TextToSpeech? = null
    private var recognitionLanguage = "bn-BD"
    private var useOnDeviceRecognition: Boolean? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale("bn", "BD")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAssistant()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_CANCEL_PENDING) {
            updateNotification("Request cancelled • say Hey Galaxy")
            return START_STICKY
        }
        if (intent?.action == ACTION_CONFIRM_MEMORY_DELETE) {
            val id = getSharedPreferences("galaxy_runtime", MODE_PRIVATE).getLong(PREF_PENDING_DELETE, -1L)
            if (id > 0L && GalaxyMemoryStore.delete(this, id)) {
                updateNotification("Memory deleted • say Hey Galaxy")
                tts?.speak("মেমোরিটি মুছে ফেলা হয়েছে।", TextToSpeech.QUEUE_FLUSH, null, "memory-deleted")
                GalaxyVoiceHistory.add(this, "Delete memory", "Deleted after confirmation")
            } else updateNotification("Memory could not be deleted")
            getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().remove(PREF_PENDING_DELETE).apply()
            return START_STICKY
        }
        if (intent?.action == ACTION_CANCEL_MEMORY_DELETE) {
            getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().remove(PREF_PENDING_DELETE).apply()
            updateNotification("Memory deletion cancelled")
            tts?.speak("মেমোরি মুছে ফেলা বাতিল করা হয়েছে।", TextToSpeech.QUEUE_FLUSH, null, "memory-delete-cancelled")
            return START_STICKY
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            updateNotification("Microphone permission required • open Galaxy to grant access")
            getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().putBoolean("voice_running", false).apply()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        stopped = false
        recognitionLanguage = "bn-BD"
        useOnDeviceRecognition = null
        getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().putBoolean("voice_running", true).apply()
        startForeground(NOTIFICATION_ID, buildNotification("Galaxy is active • say Hey Galaxy"))
        startRecognition()
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Galaxy voice assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Visible microphone session while Hey Galaxy listening is enabled"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openPending = PendingIntent.getActivity(this, 4108, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, GalaxyVoiceWakeService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(this, 4109, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Galaxy Voice Assistant")
            .setContentText(message)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Stop listening", stopPending)
            .build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun startRecognition() {
        if (stopped || isListening) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition unavailable • install or enable a speech service")
            getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().putBoolean("voice_running", false).apply()
            stopAssistant()
            return
        }
        try {
            if (recognizer == null) {
                if (useOnDeviceRecognition == null) {
                    useOnDeviceRecognition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
                }
                recognizer = if (useOnDeviceRecognition == true) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(this)
                }
                recognizer?.setRecognitionListener(listener)
            }
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recognitionLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            isListening = true
            recognizer?.startListening(request)
        } catch (_: SecurityException) {
            isListening = false
            updateNotification("Microphone permission is required • open Galaxy")
            stopAssistant()
        } catch (_: Exception) {
            isListening = false
            scheduleRestart(2500)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            updateNotification("Listening in background • say Hey Galaxy")
        }
        override fun onBeginningOfSpeech() { }
        override fun onRmsChanged(rmsdB: Float) { }
        override fun onBufferReceived(buffer: ByteArray?) { }
        override fun onEndOfSpeech() { }
        override fun onError(error: Int) {
            isListening = false
            if (stopped) return
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    updateNotification("Microphone permission revoked • stop listening")
                    stopAssistant()
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    updateNotification("Speech service busy • retrying shortly")
                    scheduleRestart(2500)
                }
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> {
                    updateNotification("Speech service disconnected • reconnecting")
                    try { recognizer?.destroy() } catch (_: Exception) { }
                    recognizer = null
                    scheduleRestart(1800)
                }
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                    if (useOnDeviceRecognition == true) {
                        useOnDeviceRecognition = false
                        updateNotification("On-device speech unavailable • trying system speech service")
                        try { recognizer?.destroy() } catch (_: Exception) { }
                        recognizer = null
                        scheduleRestart(900)
                    } else if (recognitionLanguage != "en-US") {
                        recognitionLanguage = "en-US"
                        updateNotification("Bangla recognition unavailable • switching to English")
                        try { recognizer?.destroy() } catch (_: Exception) { }
                        recognizer = null
                        scheduleRestart(900)
                    } else {
                        updateNotification("Speech language unavailable • check device speech settings")
                        scheduleRestart(5000)
                    }
                }
                else -> {
                    updateNotification("Voice recognition interrupted • retrying")
                    scheduleRestart(1600)
                }
            }
        }
        override fun onResults(results: Bundle?) {
            isListening = false
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            processPhrase(heard)
            if (!stopped) scheduleRestart(700)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val heard = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (isWakePhrase(heard)) updateNotification("Galaxy activated • finishing your command…")
        }
        override fun onEvent(eventType: Int, params: Bundle?) { }
    }

    private fun isWakePhrase(text: String): Boolean {
        val value = text.lowercase(Locale.ROOT)
        return value.contains("hey galaxy") || value.contains("হেই গ্যালাক্সি") || value.contains("হে গ্যালাক্সি")
    }

    private fun processPhrase(text: String) {
        if (text.isBlank()) return
        val hasWakePhrase = isWakePhrase(text)
        val command = if (hasWakePhrase) {
            text.lowercase(Locale.ROOT)
                .replace("hey galaxy", "")
                .replace("হেই গ্যালাক্সি", "")
                .replace("হে গ্যালাক্সি", "")
                .trim()
        } else if (wakeArmed) {
            text.trim()
        } else {
            return
        }

        if (hasWakePhrase && command.isBlank()) {
            wakeArmed = true
            updateNotification("Galaxy is ready • বলো, কী করতে হবে?")
            tts?.speak("জি, বলো।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-ready")
            handler.removeCallbacks(wakeTimeout)
            handler.postDelayed(wakeTimeout, 8000)
            return
        }

        wakeArmed = false
        handler.removeCallbacks(wakeTimeout)
        GalaxyVoiceHistory.add(this, command, "Command received")
        val shutdownText = command.lowercase(Locale.ROOT).trim()
        val shutdownPhrases = listOf(
            "shutdown your system", "shut down your system",
            "shutdown galaxy", "shut down galaxy",
            "shutdown", "shut down",
            "বন্ধ কর", "বন্ধ হয়ে যাও", "বন্ধ হয়ে যাও", "সব বন্ধ"
        )
        if (shutdownPhrases.any { shutdownText.contains(it) }) {
            GalaxyVoiceHistory.add(this, command, "Galaxy shutdown")
            updateNotification("Galaxy is shutting down…")
            stopAssistant()
            return
        }
        if (handleMemoryCommand(command)) return

        when {
            command.contains("wifi", true) || command.contains("wi-fi", true) || command.contains("ওয়াইফাই") || command.contains("ওয়াইফাই") -> {
                requestSettingsConfirmation("Wi-Fi", Intent("android.settings.panel.action.WIFI"))
            }
            command.contains("bluetooth", true) || command.contains("ব্লুটুথ") -> {
                requestSettingsConfirmation("Bluetooth", Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            command.contains("location", true) || command.contains("লোকেশন") -> {
                requestSettingsConfirmation("Location", Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            command.contains("dark mode", true) || command.contains("dark theme", true) || command.contains("ডার্ক মোড") -> {
                requestSettingsConfirmation("Display / Dark mode", Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS))
            }
            command.contains("dnd", true) || command.contains("do not disturb", true) || command.contains("ডু নট ডিস্টার্ব") -> {
                requestSettingsConfirmation("Do Not Disturb access", Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            command.contains("airplane", true) || command.contains("এয়ারপ্লেন") || command.contains("এয়ারপ্লেন") -> {
                requestSettingsConfirmation("Airplane mode settings", Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS))
            }
            command.contains("settings", true) || command.contains("সেটিংস") -> {
                requestSettingsConfirmation("Android Settings", Intent(android.provider.Settings.ACTION_SETTINGS))
            }
            command.contains("youtube", true) || command.contains("ইউটিউব") -> {
                val launch = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com"))
                requestSettingsConfirmation("Open YouTube", launch)
            }
            command.contains("camera", true) || command.contains("ক্যামেরা") -> {
                val launch = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                showActionNotification("Camera খুলতে notification থেকে Run action চাপো", launch)
                tts?.speak("ক্যামেরা খুলতে notification থেকে Run action চাপো।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-camera")
            }
            command.contains("phone", true) || command.contains("dialer", true) || command.contains("ফোন", true) || command.contains("কল", true) -> {
                val launch = Intent(Intent.ACTION_DIAL)
                showActionNotification("Phone dialer খুলতে Run action চাপো", launch)
                tts?.speak("Phone dialer খুলতে notification থেকে Run action চাপো।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-dialer")
            }
            command.contains("browser", true) || command.contains("ব্রাউজার") -> {
                val launch = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                showActionNotification("Browser খুলতে Run action চাপো", launch)
                tts?.speak("Browser খুলতে notification থেকে Run action চাপো।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-browser")
            }
            command.contains("maps", true) || command.contains("map", true) || command.contains("ম্যাপ") -> {
                val launch = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=nearby places"))
                showActionNotification("Nearby Maps খুলতে Run action চাপো", launch)
                tts?.speak("Nearby Maps খুলতে notification থেকে Run action চাপো।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-maps")
            }
            else -> {
                GalaxyVoiceHistory.add(this, command, "Unsupported command")
                updateNotification("Command not supported yet • $command".take(180))
                tts?.speak("এই কমান্ডটি এখনো সমর্থিত নয়। Voice Studio-তে supported command দেখে আবার বলো।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-unsupported")
            }
        }
    }


    private fun speakMemory(message: String, notification: String = message) {
        updateNotification(notification.take(180))
        tts?.speak(message.take(3500), TextToSpeech.QUEUE_FLUSH, null, "galaxy-memory")
    }

    private fun handleMemoryCommand(rawCommand: String): Boolean {
        val command = rawCommand.trim()
        val lower = command.lowercase(Locale.ROOT)
        fun capture(prefixes: List<String>): String? {
            val prefix = prefixes.firstOrNull { lower.startsWith(it) } ?: return null
            return command.drop(prefix.length).trim().takeIf { it.isNotBlank() }
        }

        val project = capture(listOf("save project update ", "project update মনে রাখ ", "প্রজেক্ট আপডেট মনে রাখ "))
        if (project != null) {
            val ok = GalaxyMemoryStore.add(this, "PROJECT", project)
            speakMemory(if (ok) "Project update local memory-তে save করেছি।" else "Project update save করা যায়নি।")
            GalaxyVoiceHistory.add(this, command, if (ok) "Project memory saved" else "Memory save failed")
            return true
        }
        val task = capture(listOf("remember my task ", "save task ", "আমার কাজ মনে রাখ ", "টাস্ক মনে রাখ "))
        if (task != null) {
            val ok = GalaxyMemoryStore.add(this, "TASK", task)
            speakMemory(if (ok) "Task local memory-তে save করেছি।" else "Task save করা যায়নি।")
            GalaxyVoiceHistory.add(this, command, if (ok) "Task memory saved" else "Memory save failed")
            return true
        }
        val research = capture(listOf("save research note ", "research মনে রাখ ", "রিসার্চ নোট মনে রাখ "))
        if (research != null) {
            val ok = GalaxyMemoryStore.add(this, "RESEARCH", research)
            speakMemory(if (ok) "Research note save করেছি।" else "Research note save করা যায়নি।")
            GalaxyVoiceHistory.add(this, command, if (ok) "Research memory saved" else "Memory save failed")
            return true
        }
        val remembered = capture(listOf("remember that ", "remember this: ", "remember this ", "remember ",
            "মনে রাখ যে ", "মনে রাখো যে ", "এটা মনে রাখ ", "এটা মনে রাখো ", "মনে রাখ ", "মনে রেখো "))
        if (remembered != null) {
            val category = if (lower.contains("i am ") || lower.contains("আমি ")) "PERSONAL" else "NOTES"
            val ok = GalaxyMemoryStore.add(this, category, remembered)
            speakMemory(if (ok) "ঠিক আছে, কথাটি এই ফোনের Galaxy memory-তে save করেছি।" else "Memory save করা যায়নি।")
            GalaxyVoiceHistory.add(this, command, if (ok) "Memory saved: $category" else "Memory save failed")
            return true
        }
        if (lower == "remember this" || lower == "মনে রাখ" || lower == "এটা মনে রাখ" || lower == "মনে রেখো") {
            wakeArmed = true
            updateNotification("বল, কী মনে রাখব? এখন বলো…")
            tts?.speak("কী মনে রাখব? এখন বলো।", TextToSpeech.QUEUE_FLUSH, null, "memory-capture")
            handler.removeCallbacks(wakeTimeout)
            handler.postDelayed(wakeTimeout, 10000)
            return true
        }
        if (lower.contains("what do you remember about me") ||
            lower.contains("আমার সম্পর্কে কী মনে আছে") || lower.contains("আমার ব্যাপারে কী মনে আছে")) {
            val items = GalaxyMemoryStore.search(this, "", "PERSONAL", 5).ifEmpty { GalaxyMemoryStore.recent(this, 5) }
            if (items.isEmpty()) speakMemory("এখনো কোনো local memory save করা নেই।", "No saved memories yet")
            else speakMemory("তোমার local memory থেকে পেয়েছি: " + items.joinToString(". ") { it.content },
                "Found ${items.size} saved memories")
            GalaxyVoiceHistory.add(this, command, "Recalled local memories")
            return true
        }
        val categoryRequests = listOf(
            Triple("show project memories", "PROJECT", "প্রজেক্ট"),
            Triple("show my projects", "PROJECT", "প্রজেক্ট"),
            Triple("show tasks", "TASK", "টাস্ক"),
            Triple("show my tasks", "TASK", "টাস্ক"),
            Triple("show research notes", "RESEARCH", "রিসার্চ"),
            Triple("show personal memories", "PERSONAL", "ব্যক্তিগত"),
            Triple("প্রজেক্টগুলো দেখাও", "PROJECT", "প্রজেক্ট"),
            Triple("আমার প্রজেক্ট দেখাও", "PROJECT", "প্রজেক্ট"),
            Triple("আমার কাজগুলো দেখাও", "TASK", "টাস্ক"),
            Triple("টাস্ক দেখাও", "TASK", "টাস্ক"),
            Triple("রিসার্চ নোট দেখাও", "RESEARCH", "রিসার্চ"),
            Triple("ব্যক্তিগত তথ্য দেখাও", "PERSONAL", "ব্যক্তিগত")
        )
        val categoryRequest = categoryRequests.firstOrNull { lower == it.first }
        if (categoryRequest != null) {
            val entries = GalaxyMemoryStore.search(this, "", categoryRequest.second, 6)
            val response = if (entries.isEmpty()) "এখনো কোনো " + categoryRequest.third + " memory save করা নেই।"
                else entries.joinToString(". ") { it.content }
            speakMemory(response, if (entries.isEmpty()) "No " + categoryRequest.second.lowercase() + " memories" else entries.size.toString() + " " + categoryRequest.second.lowercase() + " memories")
            GalaxyVoiceHistory.add(this, command, "Category recall: " + categoryRequest.second + " (" + entries.size + ")")
            return true
        }
        if (lower == "show recent memories" || lower == "recent memories" ||
            lower == "সাম্প্রতিক মেমোরি দেখাও" || lower == "সাম্প্রতিক তথ্য দেখাও") {
            val entries = GalaxyMemoryStore.recent(this, 6)
            val response = if (entries.isEmpty()) "এখনো কোনো memory save করা নেই।"
                else entries.joinToString(". ") { it.content }
            speakMemory(response, "Recent memories: " + entries.size)
            GalaxyVoiceHistory.add(this, command, "Recent memory recall (" + entries.size + ")")
            return true
        }

        val searchPrefix = listOf("find memory ", "search memory ", "search my memory for ", "find my ", "look up ",
            "খুঁজে দে ", "খুঁজে দাও ", "মেমোরিতে খোঁজ ", "মেমোরি খুঁজে দাও ", "আগের ")
            .firstOrNull { lower.startsWith(it) }
        if (searchPrefix != null) {
            val matches = GalaxyMemoryStore.search(this, command.drop(searchPrefix.length).trim(), limit = 5)
            val message = if (matches.isEmpty()) "এই বিষয়ে local memory-তে কিছু পাইনি।"
                else matches.joinToString(". ") { it.content }
            speakMemory(message, if (matches.isEmpty()) "No matching local memory" else "Found ${matches.size} matching memories")
            GalaxyVoiceHistory.add(this, command, if (matches.isEmpty()) "No memory match" else "Memory search results: ${matches.size}")
            return true
        }
        val forgetPrefix = listOf("forget memory ", "forget ", "এই memory মুছে দে ", "এই মেমোরি মুছে দে ", "মনে রাখা তথ্য মুছে দে ")
            .firstOrNull { lower.startsWith(it) }
        if (forgetPrefix != null) {
            val query = command.drop(forgetPrefix.length).trim()
            if (query.isBlank() || query.equals("all", true) || query == "সব") {
                speakMemory("নিরাপত্তার জন্য voice command দিয়ে সব memory একসাথে মুছব না। নির্দিষ্ট memory বলো।")
                return true
            }
            val match = GalaxyMemoryStore.search(this, query, limit = 1).firstOrNull()
            if (match == null) speakMemory("এই কথার সঙ্গে মিলে এমন memory পাইনি।")
            else {
                getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().putLong(PREF_PENDING_DELETE, match.id).apply()
                showMemoryDeleteConfirmation(match)
                GalaxyVoiceHistory.add(this, command, "Waiting for memory deletion confirmation")
            }
            return true
        }
        return false
    }

    private fun showMemoryDeleteConfirmation(entry: GalaxyMemoryEntry) {
        val confirm = PendingIntent.getService(this, 4211,
            Intent(this, GalaxyVoiceWakeService::class.java).setAction(ACTION_CONFIRM_MEMORY_DELETE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancel = PendingIntent.getService(this, 4212,
            Intent(this, GalaxyVoiceWakeService::class.java).setAction(ACTION_CANCEL_MEMORY_DELETE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Delete this saved memory?")
            .setContentText(entry.content.take(100))
            .setOngoing(true).setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_delete, "Confirm delete", confirm)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancel)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        tts?.speak("এই মেমোরিটি মুছতে notification থেকে confirm করো, না হলে cancel করো।", TextToSpeech.QUEUE_FLUSH, null, "memory-delete-confirm")
    }

    /** Voice requests never change protected system settings directly. */
    private fun requestSettingsConfirmation(label: String, actionIntent: Intent) {
        GalaxyVoiceHistory.add(this, label, "Waiting for confirmation")
        val confirmIntent = PendingIntent.getActivity(
            this, label.hashCode(), actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this, 4112,
            Intent(this, GalaxyVoiceWakeService::class.java).setAction(ACTION_CANCEL_PENDING),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val appIntent = PendingIntent.getActivity(
            this, 4113, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Galaxy needs your confirmation")
            .setContentText("$label settings খুলব? Confirm চাপলে Android page খুলবে।")
            .setContentIntent(appIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_view, "Confirm • Open", confirmIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        tts?.speak("$label settings খুলতে notification থেকে confirm করো।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-confirm")
    }

    private val wakeTimeout = Runnable {
        wakeArmed = false
        if (!stopped) updateNotification("Listening in background • say Hey Galaxy")
    }

    private fun showActionNotification(message: String, actionIntent: Intent) {
        val pending = PendingIntent.getActivity(this, message.hashCode(), actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 4110, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Galaxy Voice Assistant")
            .setContentText(message)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_view, "Run action", pending)
            .addAction(android.R.drawable.ic_media_pause, "Stop", PendingIntent.getService(this, 4109, Intent(this, GalaxyVoiceWakeService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun scheduleRestart(delay: Long) {
        if (restartQueued || stopped) return
        restartQueued = true
        handler.postDelayed({
            restartQueued = false
            startRecognition()
        }, delay)
    }

    private fun stopAssistant() {
        stopped = true
        getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().putBoolean("voice_running", false).apply()
        handler.removeCallbacksAndMessages(null)
        isListening = false
        releaseSpeechResources()
        wakeArmed = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopped = true
        getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().putBoolean("voice_running", false).apply()
        handler.removeCallbacksAndMessages(null)
        releaseSpeechResources()
        super.onDestroy()
    }

    private fun releaseSpeechResources() {
        try { recognizer?.cancel() } catch (_: Exception) { }
        try { recognizer?.destroy() } catch (_: Exception) { }
        recognizer = null
        try { tts?.stop() } catch (_: Exception) { }
        try { tts?.shutdown() } catch (_: Exception) { }
        tts = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
