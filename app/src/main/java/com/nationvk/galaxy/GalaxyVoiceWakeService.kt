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
        const val ACTION_CONFIRM_PLAN_STEP = "com.nationvk.galaxy.voice.CONFIRM_PLAN_STEP"
        const val ACTION_CANCEL_PLAN = "com.nationvk.galaxy.voice.CANCEL_PLAN"
        const val PREF_CONVERSATION_WINDOW_SECONDS = "voice_conversation_window_seconds"
        private const val DEFAULT_CONVERSATION_WINDOW_SECONDS = 12
        private const val PREF_PLAN_STEPS = "pending_plan_steps"
        private const val PREF_PLAN_INDEX = "pending_plan_index"
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
    private var networkFailureCount = 0

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) configureSpeechVoice()
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
        if (intent?.action == ACTION_CONFIRM_PLAN_STEP) {
            runNextPlannedStep()
            return START_STICKY
        }
        if (intent?.action == ACTION_CANCEL_PLAN) {
            clearPendingPlan()
            updateNotification("Multi-step plan cancelled • say Hey Galaxy")
            speak("পরিকল্পনাটি বাতিল করা হয়েছে।", "plan-cancelled")
            return START_STICKY
        }
        if (intent?.action == ACTION_CONFIRM_MEMORY_DELETE) {
            val id = getSharedPreferences("galaxy_runtime", MODE_PRIVATE).getLong(PREF_PENDING_DELETE, -1L)
            if (id > 0L && GalaxyMemoryStore.delete(this, id)) {
                updateNotification("Memory deleted • say Hey Galaxy")
                speak("মেমোরিটি মুছে ফেলা হয়েছে।", "memory-deleted")
                GalaxyVoiceHistory.add(this, "Delete memory", "Deleted after confirmation")
            } else updateNotification("Memory could not be deleted")
            getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().remove(PREF_PENDING_DELETE).apply()
            return START_STICKY
        }
        if (intent?.action == ACTION_CANCEL_MEMORY_DELETE) {
            getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit().remove(PREF_PENDING_DELETE).apply()
            updateNotification("Memory deletion cancelled")
            speak("মেমোরি মুছে ফেলা বাতিল করা হয়েছে।", "memory-delete-cancelled")
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

    /**
     * Applies the user's saved voice preference to foreground and background speech.
     * Voice availability depends on the Android TTS engine installed on the device.
     */
    private fun configureSpeechVoice() {
        val engine = tts ?: return
        val preferred = getSharedPreferences("galaxy_preferences", MODE_PRIVATE)
            .getString("voice_gender", "Male") ?: "Male"
        engine.setLanguage(Locale("bn", "BD"))
        val voices = engine.voices ?: emptySet()
        val genderToken = if (preferred.equals("Female", ignoreCase = true)) "female" else "male"
        val matchingGender = voices.filter { it.name.contains(genderToken, ignoreCase = true) }
        val bangladeshBengaliVoice = matchingGender.firstOrNull {
            it.locale.language.equals("bn", ignoreCase = true) &&
                it.locale.country.equals("BD", ignoreCase = true)
        }
        val indianBengaliVoice = matchingGender.firstOrNull {
            it.locale.language.equals("bn", ignoreCase = true) &&
                it.locale.country.equals("IN", ignoreCase = true)
        }
        val anyBengaliVoice = matchingGender.firstOrNull {
            it.locale.language.equals("bn", ignoreCase = true)
        }
        val selected = bangladeshBengaliVoice ?: indianBengaliVoice ?: anyBengaliVoice
        if (selected != null) engine.voice = selected
    }

    private fun speak(message: String, utteranceId: String) {
        configureSpeechVoice()
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
            networkFailureCount = 0
            val engine = if (useOnDeviceRecognition == true) "On-device" else "System"
            updateNotification(if (wakeArmed) "$engine recognition • conversation mode • speak next command" else "$engine recognition • $recognitionLanguage • say Hey Galaxy")
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
                SpeechRecognizer.ERROR_AUDIO -> {
                    updateNotification("Microphone audio error • checking again")
                    try { recognizer?.destroy() } catch (_: Exception) { }
                    recognizer = null
                    scheduleRestart(1800)
                }
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    networkFailureCount = (networkFailureCount + 1).coerceAtMost(5)
                    val retryDelay = (3000L * (1L shl (networkFailureCount - 1))).coerceAtMost(30000L)
                    updateNotification("Speech network unavailable • retry in " + (retryDelay / 1000) + "s")
                    try { recognizer?.destroy() } catch (_: Exception) { }
                    recognizer = null
                    scheduleRestart(retryDelay)
                }
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    updateNotification(if (wakeArmed) "Didn't catch that • say your command again" else "Listening in background • say Hey Galaxy")
                    scheduleRestart(900)
                }
                SpeechRecognizer.ERROR_SERVER -> {
                    updateNotification("Speech server error • reconnecting")
                    try { recognizer?.destroy() } catch (_: Exception) { }
                    recognizer = null
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
            speak("জি, বলো।", "galaxy-ready")
            handler.removeCallbacks(wakeTimeout)
            handler.postDelayed(wakeTimeout, conversationWindowMs())
            return
        }

        val stopConversationPhrases = listOf("stop conversation", "end conversation", "cancel follow up", "থামো", "আর শুনবে না", "কথা বলা বন্ধ", "ফলোআপ বন্ধ")
        if (stopConversationPhrases.any { command.lowercase(Locale.ROOT).contains(it) }) {
            wakeArmed = false
            handler.removeCallbacks(wakeTimeout)
            updateNotification("Conversation mode ended • say Hey Galaxy")
            speak("ঠিক আছে। আবার কথা বলতে Hey Galaxy বলো।", "conversation-ended")
            return
        }

        // Voice confirmation is accepted only when an allow-listed multi-step plan is pending.
        // A plain "yes/no" never triggers an action without that explicit pending context.
        val runtimePrefs = getSharedPreferences("galaxy_runtime", MODE_PRIVATE)
        val pendingSteps = runtimePrefs.getString(PREF_PLAN_STEPS, null)?.split("|").orEmpty()
        val pendingIndex = runtimePrefs.getInt(PREF_PLAN_INDEX, 0)
        val hasPendingPlanStep = pendingIndex in pendingSteps.indices
        val normalizedCommand = command.lowercase(Locale.ROOT).trim().trimEnd('.', '!', '?', '।')
        val affirmative = listOf(
            "yes", "yeah", "yep", "confirm", "ঠিক আছে", "হ্যাঁ", "হ্যা",
            "অনুমতি দিলাম", "চালাও", "চালু করো", "করো", "ওটাই করো"
        ).any { normalizedCommand == it || normalizedCommand.startsWith("$it ") }
        val negative = listOf(
            "no", "nope", "cancel", "না", "বাতিল", "থাক", "করবে না",
            "চালিও না", "বন্ধ করো"
        ).any { normalizedCommand == it || normalizedCommand.startsWith("$it ") }
        // A memory suggestion is confirmed independently from task-plan confirmations.
        val memoryPrefs = runtimeMemoryPrefs()
        val pendingMemoryAt = memoryPrefs.getLong("pending_memory_suggestion_at", 0L)
        val pendingMemoryText = memoryPrefs.getString("pending_memory_suggestion", null)
        val pendingMemoryCategory = memoryPrefs.getString("pending_memory_category", "NOTES") ?: "NOTES"
        val memorySuggestionActive = !pendingMemoryText.isNullOrBlank() &&
            pendingMemoryAt > 0L && System.currentTimeMillis() - pendingMemoryAt in 0..60_000L
        if (!memorySuggestionActive && !pendingMemoryText.isNullOrBlank()) {
            memoryPrefs.edit().remove("pending_memory_suggestion").remove("pending_memory_category")
                .remove("pending_memory_suggestion_at").apply()
        }
        if (memorySuggestionActive && affirmative) {
            val saved = GalaxyMemoryStore.add(this, pendingMemoryCategory, pendingMemoryText)
            memoryPrefs.edit().remove("pending_memory_suggestion").remove("pending_memory_category")
                .remove("pending_memory_suggestion_at").apply()
            speakMemory(if (saved) "ঠিক আছে, তোমার অনুমতিতে তথ্যটি Memory Vault-এ সংরক্ষণ করেছি।" else "তথ্যটি সংরক্ষণ করা যায়নি বা আগে থেকেই আছে।")
            GalaxyVoiceHistory.add(this, command, if (saved) "Suggested memory accepted" else "Suggested memory not saved")
            return
        }
        if (memorySuggestionActive && negative) {
            runtimeMemoryPrefs().edit().remove("pending_memory_suggestion").remove("pending_memory_category").apply()
            speakMemory("ঠিক আছে, তথ্যটি সংরক্ষণ করছি না।")
            GalaxyVoiceHistory.add(this, command, "Suggested memory declined")
            return
        }

        if (hasPendingPlanStep && affirmative) {
            GalaxyVoiceHistory.add(this, command, "Voice-confirmed pending plan step")
            updateNotification("Voice confirmation received • launching approved step")
            speak("ঠিক আছে, নিশ্চিতকরণের পর ধাপটি চালু করছি।", "voice-plan-confirm")
            runNextPlannedStep()
            return
        }
        if (hasPendingPlanStep && negative) {
            clearPendingPlan()
            GalaxyVoiceHistory.add(this, command, "Pending plan cancelled by voice")
            updateNotification("Plan cancelled by voice • say Hey Galaxy")
            speak("ঠিক আছে, পরিকল্পনাটি বাতিল করেছি।", "voice-plan-cancel")
            return
        }

        // Keep a short hands-free follow-up window after every accepted command.
        // The user can speak the next instruction without repeating the wake phrase.
        wakeArmed = true
        handler.removeCallbacks(wakeTimeout)
        handler.postDelayed(wakeTimeout, conversationWindowMs())
        updateNotification("Galaxy conversation mode • speak your next command")
        GalaxyVoiceHistory.add(this, command, "Command received • follow-up window active")
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
        if (handleMessageCommand(command)) return

        val localAreaRequest = command.contains("মহাখালী", true) || command.contains("mohakhali", true)
        val localUpdateRequest = listOf("খবর", "news", "traffic", "যানজট", "weather", "আবহাওয়া", "আবহাওয়া", "জব", "চাকরি", "tuition", "টিউশন", "alert", "আপডেট").any { command.contains(it, true) }
        if (localAreaRequest && localUpdateRequest) {
            val openHub = Intent(this, MainActivity::class.java)
                .putExtra("galaxy_widget_tab", "LOCAL")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            runCatching { startActivity(openHub) }
            speak("মহাখালী Local Hub খুলছি। সেখানে এলাকার খবর ও আপডেট পাবে।", "mohakhali-local-hub")
            GalaxyVoiceHistory.add(this, command, "Opened Mohakhali Local Hub")
            return
        }

        val multiStepPlan = GalaxyTaskPlanner.createPlan(command)
        if (multiStepPlan != null && multiStepPlan.steps.size > 1) {
            if (multiStepPlan.hasUnknownSteps) {
                speak("পরিকল্পনার কিছু ধাপ এখনো সমর্থিত নয়। কোনো কাজ চালানো হবে না।", "plan-unsupported")
                GalaxyVoiceHistory.add(this, command, "Plan rejected: unsupported step")
            } else {
                savePendingPlan(multiStepPlan.steps.map { it.action.name })
                showNextPlanConfirmation()
                speak("একাধিক ধাপের পরিকল্পনা তৈরি করেছি। প্রতিটি ধাপ চালাতে notification থেকে confirm করতে হবে।", "plan-ready")
                GalaxyVoiceHistory.add(this, command, "Multi-step plan waiting for step-by-step confirmation")
            }
            return
        }

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
                speak("ক্যামেরা খুলতে notification থেকে Run action চাপো।", "galaxy-camera")
            }
            (command.contains("phone", true) || command.contains("dialer", true) || command.contains("ফোন", true) || command.contains("কল", true)) &&
                !listOf("status", "health", "অবস্থা", "কেমন", "স্বাস্থ্য").any { command.contains(it, true) } -> {
                val launch = Intent(Intent.ACTION_DIAL)
                showActionNotification("Phone dialer খুলতে Run action চাপো", launch)
                speak("Phone dialer খুলতে notification থেকে Run action চাপো।", "galaxy-dialer")
            }
            command.contains("browser", true) || command.contains("ব্রাউজার") -> {
                val launch = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                showActionNotification("Browser খুলতে Run action চাপো", launch)
                speak("Browser খুলতে notification থেকে Run action চাপো।", "galaxy-browser")
            }
            command.contains("maps", true) || command.contains("map", true) || command.contains("ম্যাপ") -> {
                val launch = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=nearby places"))
                showActionNotification("Nearby Maps খুলতে Run action চাপো", launch)
                speak("Nearby Maps খুলতে notification থেকে Run action চাপো।", "galaxy-maps")
            }
            else -> {
                val planned = GalaxyTaskPlanner.createPlan(command)
                if (planned != null && planned.steps.size == 1 && planned.steps.first().supported) {
                    offerPlannedAction(planned.steps.first())
                } else {
                    val brainReply = GalaxyBrainEngine.respond(this, command)
                    if (brainReply != null) {
                        GalaxyConversationMemory.saveTurn(this, command, brainReply.spokenText)
                        GalaxyVoiceHistory.add(this, command, brainReply.status)
                        val memorySuggestion = buildMemorySuggestion(command)
                        if (memorySuggestion != null) {
                            runtimeMemoryPrefs().edit()
                                .putString("pending_memory_suggestion", memorySuggestion.second)
                                .putString("pending_memory_category", memorySuggestion.first)
                                .putLong("pending_memory_suggestion_at", System.currentTimeMillis())
                                .apply()
                            updateNotification("Galaxy has a memory suggestion • say yes or no")
                            speak("একটি গুরুত্বপূর্ণ তথ্য মনে রাখার মতো মনে হচ্ছে। এটি Memory Vault-এ সংরক্ষণ করব? হ্যাঁ অথবা না বলো।", "memory-suggestion")
                        } else {
                            updateNotification(brainReply.status.take(180))
                            speak(brainReply.spokenText, "galaxy-brain")
                        }
                    } else {
                        GalaxyVoiceHistory.add(this, command, "Needs clarification")
                        updateNotification("Galaxy needs more details • $command".take(180))
                        speak("কাজটি বুঝতে আরও একটু বিস্তারিত বলো।", "galaxy-clarify")
                    }
                }
            }
        }
    }


    private fun runtimeMemoryPrefs() = getSharedPreferences("galaxy_runtime", MODE_PRIVATE)

    /**
     * Conservative, on-device candidate detection. Nothing is saved until the user says yes.
     * Sensitive-topic statements are deliberately excluded from suggestions.
     */
    private fun buildMemorySuggestion(raw: String): Pair<String, String>? {
        val value = raw.trim()
        val q = value.lowercase(Locale.ROOT)
        if (value.length !in 24..500) return null
        val sensitive = listOf("password", "পাসওয়ার্ড", "পাসওয়ার্ড", "otp", "pin code", "ব্যাংক", "bank account",
            "রোগ", "অসুস্থ", "ওষুধ", "medicine", "ধর্ম", "রাজনীতি", "politic", "sexual", "যৌন")
        if (sensitive.any { q.contains(it) }) return null
        val firstPerson = listOf("আমি", "আমার", "i am", "i'm", "my ")
        if (firstPerson.none { q.contains(it) }) return null
        val category = when {
            listOf("project", "প্রজেক্ট", "প্রকল্প", "কাজ করছি").any { q.contains(it) } -> "PROJECT"
            listOf("পছন্দ", "ভালো লাগে", "prefer", "i like", "my favorite", "আমার লক্ষ্য", "লক্ষ্য").any { q.contains(it) } -> "PERSONAL"
            listOf("পড়াশোনা", "পড়াশোনা", "study", "শিখছি", "শিখতে চাই", "পরিকল্পনা", "পরীক্ষা", "কোর্স").any { q.contains(it) } -> "LEARNING"
            listOf("করণীয়", "করতে হবে", "to do", "আমার কাজ", "পরের সপ্তাহ", "আগামীকাল").any { q.contains(it) } -> "TASK"
            else -> return null
        }
        return category to value
    }

    private fun handleMessageCommand(rawCommand: String): Boolean {
        val q = rawCommand.lowercase(Locale.ROOT)
        val asksAboutMessages = listOf(
            "message", "মেসেজ", "মেসেজটা", "মেসেজগুলো", "কি লিখেছে", "কী লিখেছে",
            "কি মেসেজ", "কী মেসেজ", "পড়ে শোনাও", "পড়ে শোনাও", "read message",
            "read my messages", "new message", "নতুন মেসেজ", "সব মেসেজ", "সাম্প্রতিক মেসেজ",
            "শেষের মেসেজগুলো", "কয়টা মেসেজ", "কতগুলো মেসেজ", "কতটি মেসেজ", "কে মেসেজ দিয়েছে",
            "কে মেসেজ পাঠিয়েছে", "who messaged me", "who sent message", "last 3", "last three",
            "recent messages", "all messages", "how many", "whatsapp", "messenger", "telegram",
            "instagram", "gmail", "viber", "discord", "signal", "snapchat", "teams", "imo",
            "হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ", "মেসেঞ্জার", "টেলিগ্রাম", "ইনস্টাগ্রাম", "ইমো"
        ).any { q.contains(it) }
        if (!asksAboutMessages) return false

        val allRecent = GalaxyMessageStore.read(this)
        if (allRecent.isEmpty()) {
            speakMemory("এখনো কোনো নতুন সমর্থিত মেসেজ পাওয়া যায়নি। Notification Access চালু আছে কি না দেখে নাও।")
            return true
        }
        val appAliases = mapOf(
            "whatsapp" to listOf("whatsapp"),
            "messenger" to listOf("messenger"),
            "telegram" to listOf("telegram"),
            "instagram" to listOf("instagram"),
            "gmail" to listOf("gmail"),
            "ইমো" to listOf("imo"),
            "imo" to listOf("imo"),
            "viber" to listOf("viber"),
            "line" to listOf("line"),
            "discord" to listOf("discord"),
            "signal" to listOf("signal"),
            "snapchat" to listOf("snapchat"),
            "teams" to listOf("teams"),
            "sms" to listOf("messages", "messaging", "sms"),
            "মেসেঞ্জার" to listOf("messenger"),
            "হোয়াটসঅ্যাপ" to listOf("whatsapp"),
            "হোয়াটসঅ্যাপ" to listOf("whatsapp"),
            "টেলিগ্রাম" to listOf("telegram"),
            "ইনস্টাগ্রাম" to listOf("instagram")
        )
        val requestedApp = appAliases.entries.firstOrNull { (alias, _) -> q.contains(alias) }
        val recent = if (requestedApp == null) allRecent else allRecent.filter { item ->
            requestedApp.value.any { item.appName.lowercase(Locale.ROOT).contains(it) }
        }
        if (recent.isEmpty()) {
            speakMemory("${requestedApp?.key ?: "এই অ্যাপ"} থেকে কোনো সংরক্ষিত নতুন মেসেজ পাওয়া যায়নি।")
            return true
        }
        val asksCount = listOf("কয়টা", "কতগুলো", "কতটি", "how many", "count message").any { q.contains(it) }
        val asksMultiple = listOf("সব মেসেজ", "সবগুলো মেসেজ", "শেষের মেসেজগুলো", "সাম্প্রতিক মেসেজ",
            "last 3", "last three", "recent messages", "all messages", "একাধিক মেসেজ").any { q.contains(it) }
        if (asksCount) {
            val senders = recent.map { it.sender.ifBlank { it.appName } }.distinct().take(4)
            val who = if (senders.isEmpty()) "" else " সাম্প্রতিক প্রেরক: " + senders.joinToString(", ")
            speakMemory("সংরক্ষিত সাম্প্রতিক মেসেজের সংখ্যা ${recent.size}টি.$who")
            return true
        }
        val selected = if (asksMultiple) recent.take(3) else recent.take(1)
        val answer = selected.mapIndexed { index, item ->
            val sender = item.sender.ifBlank { item.appName }
            if (item.isVoiceMessage || item.content.isBlank()) {
                "${index + 1}. $sender, ${item.appName} থেকে একটি Voice Message এসেছে।"
            } else {
                val content = item.content.replace("\\n", " ").take(900)
                "${index + 1}. $sender, ${item.appName} থেকে মেসেজ। লেখা: $content"
            }
        }.joinToString(" ")
        speakMemory(answer, if (asksMultiple) "Reading latest ${selected.size} messages" else "Reading latest message")
        GalaxyVoiceHistory.add(this, rawCommand, "Read ${selected.size} recent message(s)")
        return true
    }

    private fun speakMemory(message: String, notification: String = message) {
        updateNotification(notification.take(180))
        speak(message.take(3500), "galaxy-memory")
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
            speak("কী মনে রাখব? এখন বলো।", "memory-capture")
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
        speak("এই মেমোরিটি মুছতে notification থেকে confirm করো, না হলে cancel করো।", "memory-delete-confirm")
    }

    private fun savePendingPlan(actions: List<String>) {
        getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit()
            .putString(PREF_PLAN_STEPS, actions.joinToString("|"))
            .putInt(PREF_PLAN_INDEX, 0)
            .apply()
    }

    private fun clearPendingPlan() {
        getSharedPreferences("galaxy_runtime", MODE_PRIVATE).edit()
            .remove(PREF_PLAN_STEPS)
            .remove(PREF_PLAN_INDEX)
            .apply()
    }

    private fun showNextPlanConfirmation() {
        val prefs = getSharedPreferences("galaxy_runtime", MODE_PRIVATE)
        val actions = prefs.getString(PREF_PLAN_STEPS, null)?.split("|").orEmpty()
        val index = prefs.getInt(PREF_PLAN_INDEX, 0)
        if (actions.isEmpty() || index !in actions.indices) {
            clearPendingPlan()
            updateNotification("Plan completed • say Hey Galaxy")
            speak("পরিকল্পনার সব ধাপ শেষ হয়েছে।", "plan-complete")
            return
        }
        val label = actions[index].replace("OPEN_", "").replace("_", " ").lowercase(Locale.ROOT)
        val confirm = PendingIntent.getService(this, 4310 + index,
            Intent(this, GalaxyVoiceWakeService::class.java).setAction(ACTION_CONFIRM_PLAN_STEP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancel = PendingIntent.getService(this, 4390,
            Intent(this, GalaxyVoiceWakeService::class.java).setAction(ACTION_CANCEL_PLAN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Galaxy plan • Step " + (index + 1) + "/" + actions.size)
            .setContentText("পরবর্তী ধাপ: " + label + " — চালাতে Confirm চাপো")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_view, "Confirm step", confirm)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel plan", cancel)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun runNextPlannedStep() {
        val prefs = getSharedPreferences("galaxy_runtime", MODE_PRIVATE)
        val actions = prefs.getString(PREF_PLAN_STEPS, null)?.split("|").orEmpty()
        val index = prefs.getInt(PREF_PLAN_INDEX, 0)
        val actionName = actions.getOrNull(index)
        if (actionName == null) {
            clearPendingPlan()
            speak("চালানোর মতো কোনো ধাপ বাকি নেই।", "plan-empty")
            return
        }
        val intent = when (runCatching { GalaxyTaskPlanner.ActionType.valueOf(actionName) }.getOrNull()) {
            GalaxyTaskPlanner.ActionType.OPEN_SETTINGS -> Intent(android.provider.Settings.ACTION_SETTINGS)
            GalaxyTaskPlanner.ActionType.OPEN_YOUTUBE -> packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com"))
            GalaxyTaskPlanner.ActionType.OPEN_BROWSER -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
            GalaxyTaskPlanner.ActionType.OPEN_CAMERA -> Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            GalaxyTaskPlanner.ActionType.OPEN_DIALER -> Intent(Intent.ACTION_DIAL)
            else -> null
        }
        if (intent == null) {
            clearPendingPlan()
            speak("এই ধাপটি চালানো যাচ্ছে না। পরিকল্পনা বন্ধ করেছি।", "plan-failed")
            return
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            prefs.edit().putInt(PREF_PLAN_INDEX, index + 1).apply()
            handler.postDelayed({ showNextPlanConfirmation() }, 1200)
        } catch (_: Exception) {
            clearPendingPlan()
            updateNotification("Could not launch step • plan cancelled")
            speak("এই ধাপটি চালু করা যায়নি। নিরাপত্তার জন্য পরিকল্পনা বন্ধ করেছি।", "plan-launch-failed")
        }
    }

    /**
     * Presents one allow-listed app launch for explicit notification confirmation.
     * This method only prepares a PendingIntent; the app is not launched until Run action is tapped.
     */
    private fun offerPlannedAction(step: GalaxyTaskPlanner.PlannedStep) {
        val action = when (step.action) {
            GalaxyTaskPlanner.ActionType.OPEN_SETTINGS ->
                Intent(android.provider.Settings.ACTION_SETTINGS)
            GalaxyTaskPlanner.ActionType.OPEN_YOUTUBE ->
                packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com"))
            GalaxyTaskPlanner.ActionType.OPEN_BROWSER ->
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
            GalaxyTaskPlanner.ActionType.OPEN_CAMERA ->
                Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            GalaxyTaskPlanner.ActionType.OPEN_DIALER ->
                Intent(Intent.ACTION_DIAL)
            GalaxyTaskPlanner.ActionType.UNKNOWN -> return
        }
        val label = when (step.action) {
            GalaxyTaskPlanner.ActionType.OPEN_SETTINGS -> "Android Settings"
            GalaxyTaskPlanner.ActionType.OPEN_YOUTUBE -> "YouTube"
            GalaxyTaskPlanner.ActionType.OPEN_BROWSER -> "Browser"
            GalaxyTaskPlanner.ActionType.OPEN_CAMERA -> "Camera"
            GalaxyTaskPlanner.ActionType.OPEN_DIALER -> "Phone dialer"
            GalaxyTaskPlanner.ActionType.UNKNOWN -> return
        }
        if (step.action == GalaxyTaskPlanner.ActionType.OPEN_SETTINGS) {
            requestSettingsConfirmation(label, action)
        } else {
            showActionNotification("$label খুলতে notification থেকে Run action চাপো", action)
            speak("$label খুলতে notification থেকে Run action চাপো।", "galaxy-planned-action")
        }
        GalaxyVoiceHistory.add(this, step.originalInstruction, "Waiting for explicit action confirmation")
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
        speak("$label settings খুলতে notification থেকে confirm করো।", "galaxy-confirm")
    }

    private fun conversationWindowMs(): Long =
        getSharedPreferences("galaxy_preferences", MODE_PRIVATE)
            .getInt(PREF_CONVERSATION_WINDOW_SECONDS, DEFAULT_CONVERSATION_WINDOW_SECONDS)
            .coerceIn(8, 30) * 1000L

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
