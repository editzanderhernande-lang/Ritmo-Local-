package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.player.PlaybackService

class WidgetPlaybackReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceIntent = Intent(context, PlaybackService::class.java).apply {
            this.action = action
        }
        try {
            context.startService(serviceIntent)
        } catch (_: Exception) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(openIntent)
        }
    }
}

class RitmoCompactWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        RitmoWidgetUpdater.updateCompactWidgets(context, appWidgetManager, appWidgetIds)
    }
}

class RitmoExpandedWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        RitmoWidgetUpdater.updateExpandedWidgets(context, appWidgetManager, appWidgetIds)
    }
}

object RitmoWidgetUpdater {
    private const val PREFS_NAME = "ritmo_widget_state"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_IS_FAV = "is_fav"
    private const val KEY_ART_URI = "art_uri"

    fun updateAllWidgets(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        isFavorite: Boolean,
        artUriString: String?
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TITLE, title)
            .putString(KEY_ARTIST, artist)
            .putBoolean(KEY_IS_PLAYING, isPlaying)
            .putBoolean(KEY_IS_FAV, isFavorite)
            .putString(KEY_ART_URI, artUriString)
            .apply()

        val manager = AppWidgetManager.getInstance(context)
        val compactIds = manager.getAppWidgetIds(ComponentName(context, RitmoCompactWidgetProvider::class.java))
        if (compactIds.isNotEmpty()) {
            updateCompactWidgets(context, manager, compactIds)
        }
        val expandedIds = manager.getAppWidgetIds(ComponentName(context, RitmoExpandedWidgetProvider::class.java))
        if (expandedIds.isNotEmpty()) {
            updateExpandedWidgets(context, manager, expandedIds)
        }
    }

    fun updateCompactWidgets(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "").orEmpty().ifBlank { "RitmoLocal" }
        val artist = prefs.getString(KEY_ARTIST, "").orEmpty().ifBlank { "Toca para reproducir" }
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        val artUri = prefs.getString(KEY_ART_URI, null)

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_mini_player_compact)
            bindCommon(context, views, title, artist, isPlaying, artUri)
            views.setOnClickPendingIntent(R.id.widget_compact_root, openAppPendingIntent(context))
            manager.updateAppWidget(id, views)
        }
    }

    fun updateExpandedWidgets(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "").orEmpty().ifBlank { "RitmoLocal" }
        val artist = prefs.getString(KEY_ARTIST, "").orEmpty().ifBlank { "Música local sin conexión" }
        val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
        val isFav = prefs.getBoolean(KEY_IS_FAV, false)
        val artUri = prefs.getString(KEY_ART_URI, null)

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_mini_player_expanded)
            bindCommon(context, views, title, artist, isPlaying, artUri)
            views.setImageViewResource(
                R.id.widget_btn_fav,
                if (isFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_fav,
                actionPendingIntent(context, PlaybackService.ACTION_WIDGET_FAV, 2004)
            )
            views.setOnClickPendingIntent(R.id.widget_expanded_root, openAppPendingIntent(context))
            manager.updateAppWidget(id, views)
        }
    }

    private fun bindCommon(
        context: Context,
        views: RemoteViews,
        title: String,
        artist: String,
        isPlaying: Boolean,
        artUri: String?
    ) {
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_artist, artist)
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )

        var appliedCustomArt = false
        if (!artUri.isNullOrBlank()) {
            try {
                context.contentResolver.openInputStream(Uri.parse(artUri))?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bmp = BitmapFactory.decodeStream(stream, null, opts)
                    if (bmp != null) {
                        views.setImageViewBitmap(R.id.widget_art, bmp)
                        appliedCustomArt = true
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (!appliedCustomArt) {
            views.setImageViewResource(R.id.widget_art, R.drawable.img_album_placeholder)
        }

        views.setOnClickPendingIntent(
            R.id.widget_btn_prev,
            actionPendingIntent(context, PlaybackService.ACTION_WIDGET_PREV, 2001)
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_play_pause,
            actionPendingIntent(context, PlaybackService.ACTION_WIDGET_PLAY_PAUSE, 2002)
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_next,
            actionPendingIntent(context, PlaybackService.ACTION_WIDGET_NEXT, 2003)
        )
    }

    private fun actionPendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, WidgetPlaybackReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
