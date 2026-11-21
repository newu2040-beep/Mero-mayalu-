package com.example.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.data.PreferencesRepository
import com.example.core.localization.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object SoulmateReminderScheduler {
    const val EXTRA_SLOT_INDEX = "extra_slot_index"
    private val DAILY_SLOTS = listOf(
        9 to 0,   // 09:00 AM
        14 to 0,  // 02:00 PM
        19 to 30  // 07:30 PM
    )

    fun syncReminders(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        DAILY_SLOTS.forEachIndexed { idx, (hour, minute) ->
            val pendingIntent = buildPendingIntent(context, idx)
            alarmManager.cancel(pendingIntent)
            if (enabled) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis() + 5_000L) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
    }

    private fun buildPendingIntent(context: Context, slotIndex: Int): PendingIntent {
        val intent = Intent(context, SoulmateReminderReceiver::class.java).apply {
            action = "com.example.ACTION_SOULMATE_REMINDER"
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            4000 + slotIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class SoulmateReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesRepository(context.applicationContext).preferencesFlow.first()
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    SoulmateReminderScheduler.syncReminders(context.applicationContext, prefs.dailyRemindersEnabled)
                } else if (prefs.dailyRemindersEnabled) {
                    val slot = intent.getIntExtra(SoulmateReminderScheduler.EXTRA_SLOT_INDEX, 0).coerceIn(0, 2)
                    MeroNotificationHelper.showSoulmateReminderNotification(
                        context = context.applicationContext,
                        slotIndex = slot,
                        isNepali = prefs.language == AppLanguage.NEPALI,
                        quietHoursEnabled = false,
                        ringtoneId = prefs.notificationRingtoneId,
                        customRingtoneUri = prefs.notificationCustomRingtoneUri
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
