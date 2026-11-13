package com.example.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.min

object MediaHelper {
    suspend fun compressUriToDataUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 480,
        quality: Int = 65
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
            val scale = min(
                1f,
                min(maxDimension.toFloat() / original.width, maxDimension.toFloat() / original.height)
            )
            val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (resized != original) resized.recycle()
            original.recycle()
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        }.getOrNull()
    }
}

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L
    private val amplitudeSamples = mutableListOf<Int>()

    fun start(): Boolean {
        return runCatching {
            val dir = File(context.cacheDir, "voice").apply { mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            amplitudeSamples.clear()
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(24000)
            mr.setAudioSamplingRate(16000)
            mr.setMaxDuration(30_000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            startTimeMs = System.currentTimeMillis()
            true
        }.getOrElse {
            cancel()
            false
        }
    }

    fun pollAmplitude(): Int {
        val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        val normalized = ((amp / 32767f) * 100f).toInt().coerceIn(10, 100)
        if (amplitudeSamples.size < 36) {
            amplitudeSamples.add(normalized)
        } else {
            amplitudeSamples.removeAt(0)
            amplitudeSamples.add(normalized)
        }
        return normalized
    }

    fun stopAndEncode(): RecordedVoiceNote? {
        return runCatching {
            recorder?.stop()
            recorder?.release()
            recorder = null
            val file = outputFile ?: return@runCatching null
            val durationSec = (((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()).coerceIn(1, 30)
            val bytes = file.readBytes()
            file.delete()
            if (bytes.isEmpty() || bytes.size > 180_000) return@runCatching null
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val samples = if (amplitudeSamples.isEmpty()) List(16) { 35 } else amplitudeSamples.toList()
            RecordedVoiceNote(
                base64Audio = "data:audio/mp4;base64,$b64",
                durationSec = durationSec,
                waveform = samples
            )
        }.getOrElse {
            cancel()
            null
        }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}

data class RecordedVoiceNote(
    val base64Audio: String,
    val durationSec: Int,
    val waveform: List<Int>
)

class VoiceNotePlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var currentId: String? = null

    fun play(messageId: String, base64DataUri: String, onComplete: () -> Unit) {
        stop()
        runCatching {
            val clean = base64DataUri.substringAfter("base64,", base64DataUri)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val dir = File(context.cacheDir, "voice").apply { mkdirs() }
            val tempFile = File(dir, "play_$messageId.m4a")
            tempFile.writeBytes(bytes)
            val mp = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    stop()
                    tempFile.delete()
                    onComplete()
                }
                prepare()
                start()
            }
            player = mp
            currentId = messageId
        }.onFailure {
            stop()
            onComplete()
        }
    }

    fun isPlaying(messageId: String): Boolean = currentId == messageId && player?.isPlaying == true

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        currentId = null
    }
}
