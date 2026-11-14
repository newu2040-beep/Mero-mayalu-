package com.example.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

sealed interface MediaUploadResult {
    data class Success(
        val dataUri: String,
        val width: Int,
        val height: Int,
        val sizeBytes: Int
    ) : MediaUploadResult

    data class Error(
        val messageEn: String,
        val messageNe: String
    ) : MediaUploadResult
}

object MediaHelper {

    val SUPPORTED_IMAGE_MIMES = arrayOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "image/*"
    )

    suspend fun processImageUriWithResult(
        context: Context,
        uri: Uri,
        hd: Boolean = false
    ): MediaUploadResult = withContext(Dispatchers.IO) {
        runCatching {
            val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
            if (mime.isNotBlank() &&
                !mime.startsWith("image/") &&
                !mime.contains("octet-stream")
            ) {
                return@withContext MediaUploadResult.Error(
                    messageEn = "Unsupported file format ($mime). Please choose a JPG, JPEG, PNG, or WEBP image.",
                    messageNe = "असमर्थित फाइल ढाँचा ($mime)। कृपया JPG, JPEG, PNG वा WEBP फोटो छान्नुहोस्।"
                )
            }

            val rawBytes = readBytesFromUri(context, uri)
                ?: return@withContext MediaUploadResult.Error(
                    messageEn = "Could not read the selected image file. Please try again or pick another photo.",
                    messageNe = "छानिएको फोटो पढ्न सकिएन। कृपया पुनः प्रयास गर्नुहोस् वा अर्को फोटो छान्नुहोस्।"
                )

            if (rawBytes.isEmpty()) {
                return@withContext MediaUploadResult.Error(
                    messageEn = "The selected image file is empty.",
                    messageNe = "छानिएको फोटो फाइल खाली छ।"
                )
            }

            val maxDimension = if (hd) 1080 else 760
            val maxBytes = if (hd) 210_000 else 135_000

            val encoded = encodeBytesToDataUri(rawBytes, maxDimension, maxBytes)
                ?: return@withContext MediaUploadResult.Error(
                    messageEn = "Could not decode image. Supported formats: JPG, JPEG, PNG, WEBP.",
                    messageNe = "फोटो प्रोसेस गर्न सकिएन। समर्थित ढाँचाहरू: JPG, JPEG, PNG, WEBP।"
                )

            MediaUploadResult.Success(
                dataUri = encoded.first,
                width = encoded.second.first,
                height = encoded.second.second,
                sizeBytes = encoded.third
            )
        }.getOrElse {
            MediaUploadResult.Error(
                messageEn = "Failed to load image. Please tap Retry or select another photo.",
                messageNe = "फोटो लोड गर्न समस्या भयो। कृपया पुनः प्रयास गर्नुहोस्।"
            )
        }
    }

    suspend fun compressUriToDataUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 760,
        maxBytes: Int = 135_000
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = readBytesFromUri(context, uri) ?: return@withContext null
            encodeBytesToDataUri(rawBytes, maxDimension, maxBytes)?.first
        }.getOrNull()
    }

    /**
     * High-Definition compression mode for profile photos, banners, and cultural moment images
     * while staying safely within Firestore document limits.
     */
    suspend fun compressUriHdDataUri(
        context: Context,
        uri: Uri
    ): String? = compressUriToDataUri(
        context = context,
        uri = uri,
        maxDimension = 1080,
        maxBytes = 210_000
    )

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) return bytes
            }
        }
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            }
        }
        return null
    }

    private fun encodeBytesToDataUri(
        rawBytes: ByteArray,
        maxDimension: Int,
        maxBytes: Int
    ): Triple<String, Pair<Int, Int>, Int>? {
        val boundsOpts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        var sampleSize = 1
        while (
            max(boundsOpts.outWidth / sampleSize, boundsOpts.outHeight / sampleSize) > maxDimension * 2
        ) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            ?: return null

        val largest = max(decoded.width, decoded.height)
        val scaled = if (largest > maxDimension) {
            val ratio = maxDimension.toFloat() / largest.toFloat()
            val targetW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
            val matrix = Matrix().apply { postScale(ratio, ratio) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else {
            decoded
        }

        // Composite transparent PNG / WEBP onto a warm white canvas so alpha never turns black in JPEG
        val opaqueBitmap = if (scaled.hasAlpha()) {
            val canvasBitmap = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)
            canvas.drawColor(Color.rgb(253, 245, 247))
            canvas.drawBitmap(scaled, 0f, 0f, null)
            canvasBitmap
        } else {
            scaled
        }

        var quality = 88
        var outputBytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            opaqueBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            outputBytes = out.toByteArray()
            quality -= 10
        } while (outputBytes.size > maxBytes && quality >= 38)

        val finalW = opaqueBitmap.width
        val finalH = opaqueBitmap.height
        val b64 = Base64.encodeToString(outputBytes, Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$b64"
        return Triple(dataUri, finalW to finalH, outputBytes.size)
    }
}

data class RecordedVoiceNote(
    val base64Audio: String,
    val durationSec: Int,
    val waveform: List<Int>
)

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: android.media.MediaRecorder? = null
    private var outputFile: java.io.File? = null
    private var startTimeMs: Long = 0L
    private val amplitudes = mutableListOf<Int>()

    fun start(): Boolean {
        cancel()
        return runCatching {
            val file = java.io.File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val mr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            mr.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(64000)
            mr.setAudioSamplingRate(22050)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            startTimeMs = System.currentTimeMillis()
            amplitudes.clear()
            true
        }.getOrDefault(false)
    }

    fun pollAmplitude(): Int {
        val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        val norm = ((raw / 32767f) * 100f).roundToInt().coerceIn(12, 100)
        amplitudes.add(norm)
        return norm
    }

    fun stopAndEncode(): RecordedVoiceNote? {
        val mr = recorder ?: return null
        recorder = null
        val durationSec = (((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()).coerceAtLeast(1)
        return runCatching {
            mr.stop()
            mr.release()
            val file = outputFile ?: return null
            val bytes = file.readBytes()
            file.delete()
            if (bytes.isEmpty()) return null
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val sampledWaveform = if (amplitudes.isEmpty()) {
                listOf(25, 45, 70, 50, 80, 60, 40, 30)
            } else {
                amplitudes.takeLast(24)
            }
            RecordedVoiceNote(
                base64Audio = "data:audio/mp4;base64,$b64",
                durationSec = durationSec,
                waveform = sampledWaveform
            )
        }.getOrNull()
    }

    fun cancel() {
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
        runCatching {
            outputFile?.delete()
        }
        outputFile = null
        amplitudes.clear()
    }
}

class VoiceNotePlayer(private val context: Context) {
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var tempFile: java.io.File? = null

    fun play(messageId: String, base64Audio: String, onFinished: () -> Unit) {
        stop()
        runCatching {
            val cleanB64 = base64Audio.substringAfter("base64,", base64Audio)
            val bytes = Base64.decode(cleanB64, Base64.DEFAULT)
            val file = java.io.File(context.cacheDir, "play_${messageId.hashCode()}.m4a")
            file.writeBytes(bytes)
            tempFile = file
            val mp = android.media.MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stop()
                    onFinished()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onFinished()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
        }.onFailure {
            stop()
            onFinished()
        }
    }

    fun stop() {
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        runCatching {
            tempFile?.delete()
        }
        tempFile = null
    }
}

