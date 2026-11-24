package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.UserPreferencesRepository
import com.example.model.AppLanguage
import com.example.model.TimeOfDaySlot
import com.example.ui.i18n.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class PlaylistReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slotWire = intent.getStringExtra(EXTRA_SLOT) ?: TimeOfDaySlot.current().wireValue
        val slot = TimeOfDaySlot.fromWire(slotWire)
        PlaylistReminderScheduler.showReminderNotification(context, slot)
    }

    companion object {
        const val EXTRA_SLOT = "extra_time_slot"
    }
}

object PlaylistReminderScheduler {

    private const val CHANNEL_ID = "ritmolocal_mix_reminders"
    private const val CHANNEL_NAME = "Playlists por horario"
    private const val REQ_MORNING = 3101
    private const val REQ_AFTERNOON = 3102
    private const val REQ_NIGHT = 3103

    fun updateSchedule(context: Context, enabled: Boolean) {
        if (enabled) {
            scheduleDailySlot(context, TimeOfDaySlot.MORNING, 8, 30, REQ_MORNING)
            scheduleDailySlot(context, TimeOfDaySlot.AFTERNOON, 14, 0, REQ_AFTERNOON)
            scheduleDailySlot(context, TimeOfDaySlot.NIGHT, 20, 30, REQ_NIGHT)
        } else {
            cancelSlot(context, TimeOfDaySlot.MORNING, REQ_MORNING)
            cancelSlot(context, TimeOfDaySlot.AFTERNOON, REQ_AFTERNOON)
            cancelSlot(context, TimeOfDaySlot.NIGHT, REQ_NIGHT)
        }
    }

    private fun scheduleDailySlot(
        context: Context,
        slot: TimeOfDaySlot,
        hour: Int,
        minute: Int,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = buildPendingIntent(context, slot, requestCode)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    private fun cancelSlot(context: Context, slot: TimeOfDaySlot, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = buildPendingIntent(context, slot, requestCode)
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(context: Context, slot: TimeOfDaySlot, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaylistReminderReceiver::class.java).apply {
            putExtra(PlaylistReminderReceiver.EXTRA_SLOT, slot.wireValue)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showReminderNotification(context: Context, slot: TimeOfDaySlot) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)
        val language = try {
            runBlocking { UserPreferencesRepository(context).appSettingsPrefs.first().appLanguage }
        } catch (_: Exception) {
            AppLanguage.ES_LATAM
        }
        val strings = stringsFor(language)
        val (title, body) = when (slot) {
            TimeOfDaySlot.MORNING -> strings.morningGreetingTitle to strings.morningGreetingBody
            TimeOfDaySlot.AFTERNOON -> strings.afternoonGreetingTitle to strings.afternoonGreetingBody
            TimeOfDaySlot.NIGHT -> strings.nightGreetingTitle to strings.nightGreetingBody
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MIX_SLOT, slot.wireValue)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            4000 + slot.ordinal,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_ritmo)
            .setColor(ContextCompat.getColor(context, R.color.ritmo_notif_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(5100 + slot.ordinal, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones de saludo y recomendación de playlist según la hora del día"
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    const val EXTRA_OPEN_MIX_SLOT = "open_mix_slot"
}
