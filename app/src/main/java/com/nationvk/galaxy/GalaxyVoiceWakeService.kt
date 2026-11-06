package com.nationvk.galaxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
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
import java.util.Locale

class GalaxyVoiceWakeService : Service() {
    companion object {
        const val ACTION_STOP = "com.nationvk.galaxy.voice.STOP"
        const val ACTION_TOGGLE = "com.nationvk.galaxy.voice.TOGGLE"
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
        stopped = false
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
            updateNotification("Speech recognition unavailable on this device")
            return
        }
        try {
            if (recognizer == null) {
                recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(this)
                }
                recognizer?.setRecognitionListener(listener)
            }
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            isListening = true
            recognizer?.startListening(request)
        } catch (_: SecurityException) {
            isListening = false
            updateNotification("Microphone permission is required • open Galaxy")
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
            if (!stopped) scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1800 else 900)
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
        when {
            command.contains("wifi", true) || command.contains("wi-fi", true) || command.contains("ওয়াইফাই") || command.contains("ওয়াইফাই") -> {
                showActionNotification("Galaxy heard: Wi-Fi settings", Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            }
            command.contains("bluetooth", true) || command.contains("ব্লুটুথ") -> {
                showActionNotification("Galaxy heard: Bluetooth settings", Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            command.contains("settings", true) || command.contains("সেটিংস") -> {
                showActionNotification("Galaxy heard: Settings", Intent(android.provider.Settings.ACTION_SETTINGS))
            }
            command.contains("youtube", true) || command.contains("ইউটিউব") -> {
                val launch = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com"))
                showActionNotification("Galaxy heard: Open YouTube", launch)
            }
            else -> {
                updateNotification("Command received: $command • more actions coming soon")
                tts?.speak("এই কাজটি এখনো শেখানো হয়নি।", TextToSpeech.QUEUE_FLUSH, null, "galaxy-unsupported")
            }
        }
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
        handler.removeCallbacksAndMessages(null)
        isListening = false
        try { recognizer?.cancel() } catch (_: Exception) { }
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        wakeArmed = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
