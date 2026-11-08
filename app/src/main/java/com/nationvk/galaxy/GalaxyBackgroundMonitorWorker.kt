package com.nationvk.galaxy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class GalaxyBackgroundMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val alerts = mutableListOf<Pair<String, String>>()
        val batteryIntent = applicationContext.registerReceiver(
            null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.let {
            val raw = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (raw >= 0 && scale > 0) raw * 100 / scale else -1
        } ?: -1
        val batteryLimit = prefs.getInt(KEY_BATTERY_LIMIT, 20).coerceIn(5, 50)
        if (level in 0..batteryLimit) alerts += "battery" to "Battery low: $level%"

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (tempTenths >= 450) alerts += "temperature" to "Battery temperature is high: ${tempTenths / 10.0} °C"

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= 29) {
            val thermal = powerManager.currentThermalStatus
            if (thermal >= PowerManager.THERMAL_STATUS_SEVERE) {
                val label = when (thermal) {
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown risk"
                    else -> "Severe"
                }
                alerts += "thermal" to "Phone thermal status: $label"
            }
        }

        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memory = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        val ramUsed = ((memory.totalMem - memory.availMem).coerceAtLeast(0L) * 100L / memory.totalMem.coerceAtLeast(1L)).toInt()
        if (memory.lowMemory || ramUsed >= 90) alerts += "memory" to "High memory pressure: $ramUsed% RAM used"

        val stat = StatFs(Environment.getDataDirectory().path)
        if (stat.availableBytes < 1024L * 1024L * 1024L) {
            alerts += "storage" to "Low storage: less than 1 GB free"
        }

        alerts.forEach { (key, message) ->
            val lastSent = prefs.getLong("last_$key", 0L)
            val now = System.currentTimeMillis()
            if (now - lastSent >= TimeUnit.HOURS.toMillis(6)) {
                postAlert(key, message)
                prefs.edit().putLong("last_$key", now).apply()
            }
        }
        return Result.success()
    }

    private fun postAlert(key: String, message: String) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Galaxy Phone Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Battery, temperature, memory and storage warnings"
                }
            )
        }
        val open = android.app.PendingIntent.getActivity(
            applicationContext, 4701,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            4700 + key.hashCode().and(0x7fff),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Galaxy Phone Alert")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        )
    }

    companion object {
        const val PREFS = "galaxy_background_monitor"
        const val KEY_ENABLED = "enabled"
        const val KEY_BATTERY_LIMIT = "battery_limit"
        const val UNIQUE_WORK = "galaxy_phone_background_monitor"
        const val CHANNEL_ID = "galaxy_phone_alerts"
    }
}
