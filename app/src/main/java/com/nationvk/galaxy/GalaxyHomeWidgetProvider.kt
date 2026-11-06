package com.nationvk.galaxy

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class GalaxyHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, manager, it) }
    }

    companion object {
        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.galaxy_home_widget)
            views.setOnClickPendingIntent(R.id.widget_open, pendingIntent(context, "HOME", 1))
            views.setOnClickPendingIntent(R.id.widget_chat, pendingIntent(context, "CHAT", 2))
            views.setOnClickPendingIntent(R.id.widget_voice, pendingIntent(context, "VOICE", 3))
            views.setOnClickPendingIntent(R.id.widget_settings, pendingIntent(context, "SETTINGS", 4))
            manager.updateAppWidget(widgetId, views)
        }

        private fun pendingIntent(context: Context, tab: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("galaxy_widget_tab", tab)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
