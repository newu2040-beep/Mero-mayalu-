package com.example.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.audio.MeroSoundAndHapticHelper
import java.time.LocalTime

object MeroNotificationHelper {
    private const val CHANNEL_INTERESTS = "mero_mayalu_interests"
    private const val CHANNEL_MESSAGES = "mero_mayalu_messages"
    private const val CHANNEL_REMINDERS = "mero_mayalu_reminders"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val interestChannel = NotificationChannel(
                CHANNEL_INTERESTS,
                "Interest Requests & Matches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when someone sends you an interest request or mutual match."
            }
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Mutual Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for new messages in mutual connections."
            }
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Soulmate Connection Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Warm daily reminders (3x/day) encouraging you to discover & connect."
            }
            nm.createNotificationChannel(interestChannel)
            nm.createNotificationChannel(messageChannel)
            nm.createNotificationChannel(reminderChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun isInQuietHours(): Boolean {
        val hour = LocalTime.now().hour
        return hour >= 22 || hour < 7
    }

    @SuppressLint("MissingPermission")
    fun showInterestNotification(
        context: Context,
        senderName: String,
        discreet: Boolean,
        quietHoursEnabled: Boolean,
        isNepali: Boolean,
        ringtoneId: String = "kathmandu_bell",
        customRingtoneUri: String = ""
    ) {
        if (quietHoursEnabled && isInQuietHours()) return
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val title = if (isNepali) "मेरो मायालु" else "Mero Mayalu"
        val body = when {
            discreet && isNepali -> "तपाईंलाई एउटा नयाँ चासो अनुरोध आएको छ।"
            discreet -> "You received a new interest request."
            isNepali -> "$senderName ले तपाईंप्रति चासो पठाउनुभयो।"
            else -> "$senderName sent you an interest request."
        }
        postNotification(
            context = context,
            channelId = CHANNEL_INTERESTS,
            notificationId = 1001 + (senderName.hashCode() and 0xFF),
            title = title,
            body = body
        )
        MeroSoundAndHapticHelper.playOneShotTone(context, "notification", ringtoneId, customRingtoneUri)
    }

    @SuppressLint("MissingPermission")
    fun showMessageNotification(
        context: Context,
        senderName: String,
        preview: String,
        discreet: Boolean,
        quietHoursEnabled: Boolean,
        isNepali: Boolean,
        ringtoneId: String = "lotus_chime",
        customRingtoneUri: String = ""
    ) {
        if (quietHoursEnabled && isInQuietHours()) return
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val title = if (discreet) {
            if (isNepali) "मेरो मायालु" else "Mero Mayalu"
        } else {
            senderName
        }
        val body = when {
            discreet && isNepali -> "नयाँ निजी सन्देश प्राप्त भयो।"
            discreet -> "You have a new private message."
            else -> preview.ifBlank { if (isNepali) "नयाँ सन्देश" else "New message" }
        }
        postNotification(
            context = context,
            channelId = CHANNEL_MESSAGES,
            notificationId = 2001 + (senderName.hashCode() and 0xFF),
            title = title,
            body = body
        )
        MeroSoundAndHapticHelper.playOneShotTone(context, "message", ringtoneId, customRingtoneUri)
    }

    @SuppressLint("MissingPermission")
    fun showSoulmateReminderNotification(
        context: Context,
        slotIndex: Int,
        isNepali: Boolean,
        quietHoursEnabled: Boolean,
        ringtoneId: String = "kathmandu_bell",
        customRingtoneUri: String = ""
    ) {
        if (quietHoursEnabled && isInQuietHours()) return
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val (title, body) = when (slotIndex) {
            0 -> if (isNepali) {
                "शुभ प्रभात! मेरो मायालु ✨" to "नयाँ बिहानीमा तपाईंको आत्मीय जीवनसाथी भेट्ने सुन्दर अवसर — प्रोफाइलहरू हेर्नुहोस् र सभ्य संवाद थाल्नुहोस्।"
            } else {
                "Good Morning from Mero Mayalu ✨" to "Someone thoughtful could be waiting to connect with you today. Check nearby Nepali profiles and share a warm hello!"
            }
            1 -> if (isNepali) {
                "दिउँसोको चिया गफ ☕ मेरो मायालु" to "तपाईंको रुचि र संस्कृति मिल्ने नयाँ सदस्यहरू सक्रिय छन्। मायालु AI योजना र विचारहरू अन्वेषण गर्नुहोस्!"
            } else {
                "Midday Chiya Check-in ☕" to "New compatible members are active nearby! Open Mero Mayalu to discover your soulmate and share respectful moments."
            }
            else -> if (isNepali) {
                "साँझको आत्मीय क्षण 🌸 मेरो मायालु" to "तपाईंको विशेष साथीसँग जोडिने समय — नयाँ सन्देश, भाइब र भेटघाट विचारहरू हेर्नुहोस्।"
            } else {
                "Evening Connection Moment 🌸" to "Unwind and meet someone genuinely aligned with your values on Mero Mayalu tonight."
            }
        }
        postNotification(
            context = context,
            channelId = CHANNEL_REMINDERS,
            notificationId = 3100 + slotIndex,
            title = title,
            body = body
        )
        MeroSoundAndHapticHelper.playOneShotTone(context, "notification", ringtoneId, customRingtoneUri)
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
