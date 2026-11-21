package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

data class MeroRingtoneOption(
    val id: String,
    val labelEn: String,
    val labelNe: String
)

object MeroRingtoneCatalog {
    const val ID_DEVICE_DEFAULT = "device_default"
    const val ID_HIMALAYAN_FLUTE = "himalayan_flute"
    const val ID_KATHMANDU_BELL = "kathmandu_bell"
    const val ID_LOTUS_CHIME = "lotus_chime"
    const val ID_SARANGI_PULSE = "sarangi_pulse"
    const val ID_CUSTOM = "custom"

    val options = listOf(
        MeroRingtoneOption(ID_DEVICE_DEFAULT, "Device Default", "मोबाइल डिफल्ट"),
        MeroRingtoneOption(ID_HIMALAYAN_FLUTE, "Himalayan Bansuri", "हिमाली बाँसुरी"),
        MeroRingtoneOption(ID_KATHMANDU_BELL, "Kathmandu Temple Bell", "काठमाडौँ घण्टी"),
        MeroRingtoneOption(ID_LOTUS_CHIME, "Lotus Dew Chime", "कमलको शीतल धुन"),
        MeroRingtoneOption(ID_SARANGI_PULSE, "Sarangi Warm Melody", "सारङ्गी मधुर धुन"),
        MeroRingtoneOption(ID_CUSTOM, "Custom Audio File", "आफ्नै अडियो फाइल")
    )

    fun labelFor(id: String, isNepali: Boolean): String {
        val found = options.firstOrNull { it.id == id } ?: options[1]
        return if (isNepali) found.labelNe else found.labelEn
    }
}

object MeroSoundAndHapticHelper {
    private var activeCallMediaPlayer: MediaPlayer? = null
    private var activeCallDefaultRingtone: Ringtone? = null
    private var oneShotPlayer: MediaPlayer? = null

    fun triggerHaptic(context: Context, enabled: Boolean = true, strong: Boolean = false, view: View? = null) {
        if (!enabled) return
        if (view != null) {
            val flag = if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
            runCatching { view.performHapticFeedback(flag) }
            return
        }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                val ms = if (strong) 45L else 18L
                val amp = if (strong) 180 else 95
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, amp))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(ms)
                }
            }
        }
    }

    suspend fun copyCustomRingtone(context: Context, channel: String, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "ringtones").apply { mkdirs() }
                val destFile = File(dir, "${channel}_custom.audio")
                val bytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val data = input.readBytes()
                    require(data.isNotEmpty()) { "Selected audio file is empty." }
                    require(data.size <= 15 * 1024 * 1024) { "Audio file must be under 15 MB." }
                    data
                } ?: throw IllegalStateException("Could not open selected audio file.")
                FileOutputStream(destFile).use { out ->
                    out.write(bytes)
                }
                Uri.fromFile(destFile).toString()
            }
        }

    fun playOneShotTone(
        context: Context,
        channel: String,
        ringtoneId: String,
        customUriString: String
    ) {
        stopOneShot()
        if (ringtoneId == MeroRingtoneCatalog.ID_DEVICE_DEFAULT) {
            runCatching {
                val type = when (channel) {
                    "call" -> RingtoneManager.TYPE_RINGTONE
                    else -> RingtoneManager.TYPE_NOTIFICATION
                }
                val uri = RingtoneManager.getDefaultUri(type)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (uri != null) {
                    RingtoneManager.getRingtone(context, uri)?.play()
                }
            }
            return
        }

        val targetUri: Uri? = if (ringtoneId == MeroRingtoneCatalog.ID_CUSTOM && customUriString.isNotBlank()) {
            Uri.parse(customUriString)
        } else {
            val file = ensureBuiltInWavFile(context, ringtoneId)
            Uri.fromFile(file)
        }

        if (targetUri == null) return
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (channel == "call") AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                            else AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, targetUri)
                setOnCompletionListener { player ->
                    runCatching { player.release() }
                    if (oneShotPlayer == player) oneShotPlayer = null
                }
                setOnErrorListener { player, _, _ ->
                    runCatching { player.release() }
                    if (oneShotPlayer == player) oneShotPlayer = null
                    true
                }
                prepare()
                start()
            }
            oneShotPlayer = mp
        }.onFailure {
            runCatching {
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (fallback != null) RingtoneManager.getRingtone(context, fallback)?.play()
            }
        }
    }

    fun startIncomingCallRingtone(
        context: Context,
        ringtoneId: String,
        customUriString: String
    ) {
        stopIncomingCallRingtone()
        if (ringtoneId == MeroRingtoneCatalog.ID_DEVICE_DEFAULT) {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (uri != null) {
                    val rt = RingtoneManager.getRingtone(context, uri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        rt?.isLooping = true
                    }
                    rt?.play()
                    activeCallDefaultRingtone = rt
                }
            }
            return
        }

        val targetUri: Uri = if (ringtoneId == MeroRingtoneCatalog.ID_CUSTOM && customUriString.isNotBlank()) {
            Uri.parse(customUriString)
        } else {
            Uri.fromFile(ensureBuiltInWavFile(context, ringtoneId))
        }

        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, targetUri)
                isLooping = true
                setOnErrorListener { player, _, _ ->
                    runCatching { player.release() }
                    if (activeCallMediaPlayer == player) activeCallMediaPlayer = null
                    true
                }
                prepare()
                start()
            }
            activeCallMediaPlayer = mp
        }
    }

    fun stopIncomingCallRingtone() {
        runCatching { activeCallMediaPlayer?.stop() }
        runCatching { activeCallMediaPlayer?.release() }
        activeCallMediaPlayer = null
        runCatching { activeCallDefaultRingtone?.stop() }
        activeCallDefaultRingtone = null
    }

    fun stopOneShot() {
        runCatching { oneShotPlayer?.stop() }
        runCatching { oneShotPlayer?.release() }
        oneShotPlayer = null
    }

    private fun ensureBuiltInWavFile(context: Context, ringtoneId: String): File {
        val dir = File(context.filesDir, "ringtones").apply { mkdirs() }
        val safeId = ringtoneId.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { MeroRingtoneCatalog.ID_LOTUS_CHIME }
        val outFile = File(dir, "builtin_$safeId.wav")
        if (outFile.exists() && outFile.length() > 1000L) return outFile

        val notesHz: List<Pair<Double, Int>> = when (safeId) {
            MeroRingtoneCatalog.ID_HIMALAYAN_FLUTE -> listOf(
                523.25 to 260, 587.33 to 260, 659.25 to 320, 783.99 to 450, 0.0 to 180
            )
            MeroRingtoneCatalog.ID_KATHMANDU_BELL -> listOf(
                880.0 to 320, 1318.5 to 420, 0.0 to 120
            )
            MeroRingtoneCatalog.ID_SARANGI_PULSE -> listOf(
                440.0 to 240, 523.25 to 240, 659.25 to 280, 587.33 to 360, 0.0 to 160
            )
            else -> listOf( // LOTUS_CHIME
                659.25 to 180, 783.99 to 220, 1046.5 to 380
            )
        }
        writeSynthesizedWav(outFile, notesHz)
        return outFile
    }

    private fun writeSynthesizedWav(file: File, notes: List<Pair<Double, Int>>) {
        val sampleRate = 22050
        val totalSamples = notes.sumOf { (_, ms) -> (sampleRate * ms) / 1000 }
        val pcm = ShortArray(totalSamples)
        var cursor = 0
        for ((freq, durationMs) in notes) {
            val count = (sampleRate * durationMs) / 1000
            for (i in 0 until count) {
                if (cursor >= pcm.size) break
                val t = i.toDouble() / sampleRate
                val attack = (i / (sampleRate * 0.015)).coerceIn(0.0, 1.0)
                val decay = exp(-3.6 * i.toDouble() / count.coerceAtLeast(1))
                val value = if (freq <= 0.0) {
                    0.0
                } else {
                    val fundamental = sin(2.0 * PI * freq * t)
                    val overtone = 0.25 * sin(2.0 * PI * (freq * 2.0) * t)
                    (fundamental + overtone) * attack * decay * 15000.0
                }
                pcm[cursor++] = value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        val dataBytesLen = pcm.size * 2
        val byteBuffer = ByteArray(44 + dataBytesLen)
        fun putStr(offset: Int, s: String) {
            s.forEachIndexed { idx, c -> byteBuffer[offset + idx] = c.code.toByte() }
        }
        fun putIntLE(offset: Int, v: Int) {
            byteBuffer[offset] = (v and 0xFF).toByte()
            byteBuffer[offset + 1] = ((v ushr 8) and 0xFF).toByte()
            byteBuffer[offset + 2] = ((v ushr 16) and 0xFF).toByte()
            byteBuffer[offset + 3] = ((v ushr 24) and 0xFF).toByte()
        }
        fun putShortLE(offset: Int, v: Int) {
            byteBuffer[offset] = (v and 0xFF).toByte()
            byteBuffer[offset + 1] = ((v ushr 8) and 0xFF).toByte()
        }
        putStr(0, "RIFF")
        putIntLE(4, 36 + dataBytesLen)
        putStr(8, "WAVE")
        putStr(12, "fmt ")
        putIntLE(16, 16)
        putShortLE(20, 1) // PCM
        putShortLE(22, 1) // Mono
        putIntLE(24, sampleRate)
        putIntLE(28, sampleRate * 2)
        putShortLE(32, 2)
        putShortLE(34, 16)
        putStr(36, "data")
        putIntLE(40, dataBytesLen)
        var outIdx = 44
        for (sample in pcm) {
            byteBuffer[outIdx++] = (sample.toInt() and 0xFF).toByte()
            byteBuffer[outIdx++] = ((sample.toInt() ushr 8) and 0xFF).toByte()
        }
        FileOutputStream(file).use { it.write(byteBuffer) }
    }
}
