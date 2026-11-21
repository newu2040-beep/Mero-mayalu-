package com.example.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
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

sealed interface VideoUploadResult {
    data class Success(
        val dataUri: String,
        val durationSec: Int,
        val sizeBytes: Int,
        val thumbnailBitmap: Bitmap?
    ) : VideoUploadResult

    data class Error(
        val messageEn: String,
        val messageNe: String
    ) : VideoUploadResult
}

object MediaHelper {

    val SUPPORTED_IMAGE_MIMES = arrayOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "image/*"
    )

    val SUPPORTED_VIDEO_MIMES = arrayOf(
        "video/mp4",
        "video/3gpp",
        "video/webm",
        "video/*"
    )

    private const val MAX_RAW_VIDEO_BYTES = 520_000

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

            val maxDimension = if (hd) 1440 else 800
            val maxBytes = if (hd) 340_000 else 150_000

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

    suspend fun processVideoUriWithResult(
        context: Context,
        uri: Uri
    ): VideoUploadResult = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        val retriever = MediaMetadataRetriever()
        try {
            val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
            if (mime.isNotBlank() && !mime.startsWith("video/") && !mime.contains("octet-stream")) {
                return@withContext VideoUploadResult.Error(
                    messageEn = "Unsupported video format ($mime). Please choose an MP4 or mobile video clip.",
                    messageNe = "असमर्थित भिडियो ढाँचा ($mime)। कृपया MP4 वा मोबाइल भिडियो छान्नुहोस्।"
                )
            }

            var durationMs = 0L
            var thumb: Bitmap? = null
            runCatching {
                retriever.setDataSource(context, uri)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                thumb = retriever.getFrameAtTime(0L)
            }

            val declaredLength = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }.getOrDefault(-1L)

            val rawBytes: ByteArray? = if (declaredLength in 1L..MAX_RAW_VIDEO_BYTES.toLong()) {
                readBytesFromUri(context, uri)
            } else {
                val trimmed = trimVideoToBudgetMp4(context, uri, MAX_RAW_VIDEO_BYTES - 25_000)
                tempFile = trimmed.first
                val bytes = trimmed.first?.takeIf { it.exists() && it.length() in 1L..MAX_RAW_VIDEO_BYTES.toLong() }?.readBytes()
                if (trimmed.second > 0L) durationMs = trimmed.second
                bytes
            }

            if (rawBytes == null || rawBytes.isEmpty()) {
                return@withContext VideoUploadResult.Error(
                    messageEn = "Video is too large to send cleanly. Please choose a shorter HD clip (under ~520 KB).",
                    messageNe = "भिडियो साइज धेरै ठूलो छ। कृपया छोटो HD भिडियो क्लिप (करिब ५२० KB भित्र) छान्नुहोस्।"
                )
            }

            val b64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            val dataUri = "data:video/mp4;base64,$b64"
            val sec = ((durationMs / 1000L).toInt()).coerceIn(1, 60)

            VideoUploadResult.Success(
                dataUri = dataUri,
                durationSec = sec,
                sizeBytes = rawBytes.size,
                thumbnailBitmap = thumb
            )
        } catch (_: Exception) {
            VideoUploadResult.Error(
                messageEn = "Could not process selected video. Please choose a short MP4 clip and retry.",
                messageNe = "छानिएको भिडियो तयार गर्न सकिएन। कृपया छोटो MP4 भिडियो छान्नुहोस्।"
            )
        } finally {
            runCatching { retriever.release() }
            runCatching { tempFile?.delete() }
        }
    }

    private fun trimVideoToBudgetMp4(
        context: Context,
        uri: Uri,
        targetSampleBytes: Int
    ): Pair<File?, Long> {
        val outFile = File(context.cacheDir, "chat_vid_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var videoTrack = -1
            var audioTrack = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val trackMime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (trackMime.startsWith("video/") && videoTrack == -1) videoTrack = i
                else if (trackMime.startsWith("audio/") && audioTrack == -1) audioTrack = i
            }
            if (videoTrack == -1) return null to 0L

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outVideoIdx = muxer.addTrack(extractor.getTrackFormat(videoTrack))
            val outAudioIdx = if (audioTrack != -1) {
                runCatching { muxer.addTrack(extractor.getTrackFormat(audioTrack)) }.getOrDefault(-1)
            } else -1

            extractor.selectTrack(videoTrack)
            if (audioTrack != -1 && outAudioIdx != -1) extractor.selectTrack(audioTrack)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val info = MediaCodec.BufferInfo()
            var writtenBytes = 0L
            var firstPts = -1L
            var lastPts = 0L
            val maxDurationUs = 15_000_000L

            while (true) {
                val trackIdx = extractor.sampleTrackIndex
                if (trackIdx < 0) break
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val pts = extractor.sampleTime
                if (firstPts < 0L) firstPts = pts
                if (writtenBytes + size > targetSampleBytes || (pts - firstPts) > maxDurationUs) {
                    if (writtenBytes > 0L) break else return null to 0L
                }
                info.offset = 0
                info.size = size
                info.presentationTimeUs = (pts - firstPts).coerceAtLeast(0L)
                info.flags = extractor.sampleFlags
                val dstIdx = if (trackIdx == videoTrack) outVideoIdx else outAudioIdx
                if (dstIdx != -1) {
                    muxer.writeSampleData(dstIdx, buffer, info)
                    writtenBytes += size
                    lastPts = pts
                }
                extractor.advance()
            }
            muxer.stop()
            val clippedMs = if (firstPts >= 0L && lastPts >= firstPts) (lastPts - firstPts) / 1000L else 1000L
            outFile to clippedMs
        } catch (_: Exception) {
            runCatching { outFile.delete() }
            null to 0L
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }

    suspend fun compressUriToDataUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 800,
        maxBytes: Int = 150_000
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = readBytesFromUri(context, uri) ?: return@withContext null
            encodeBytesToDataUri(rawBytes, maxDimension, maxBytes)?.first
        }.getOrNull()
    }

    /**
     * High-Definition compression mode for profile photos, banners, and chat HD images
     * while staying safely within Firestore document limits.
     */
    suspend fun compressUriHdDataUri(
        context: Context,
        uri: Uri
    ): String? = compressUriToDataUri(
        context = context,
        uri = uri,
        maxDimension = 1440,
        maxBytes = 340_000
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

