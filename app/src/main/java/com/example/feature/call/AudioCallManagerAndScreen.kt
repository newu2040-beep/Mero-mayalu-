package com.example.feature.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Base64
import com.example.core.audio.MeroRingtoneCatalog
import com.example.core.audio.MeroSoundAndHapticHelper
import com.example.core.localization.AppStrings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.CallSession
import com.example.core.localization.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class RealTimeCallAudioEngine(private val context: Context) {
    private val sampleRate = 16000 // Wideband 16 kHz HD Voice
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var captureJob: Job? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var isMuted: Boolean = false

    @Volatile
    var isSpeakerOn: Boolean = false
        private set

    @Volatile
    var localVoiceLevel: Int = 10
        private set

    private var lastPlayedRemoteSeq: Int = 0

    @SuppressLint("MissingPermission")
    fun startAcceptedSession(onChunkReady: (String, Int) -> Unit) {
        if (captureJob?.isActive == true) return
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return

        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.mode = AudioManager.MODE_IN_COMMUNICATION
            setSpeakerphone(false)

            val minRecBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(6400)

            val rec = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minRecBuf * 2
            )
            if (rec.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord = rec
                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = runCatching { AcousticEchoCanceler.create(rec.audioSessionId)?.apply { enabled = true } }.getOrNull()
                }
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = runCatching { NoiseSuppressor.create(rec.audioSessionId)?.apply { enabled = true } }.getOrNull()
                }
                rec.startRecording()
            }

            val minPlayBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(6400)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minPlayBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.play()
                audioTrack = track
            }
        }

        captureJob = engineScope.launch {
            val shortBuf = ShortArray(4800) // ~300ms @ 16kHz HD Voice
            var seq = 1
            while (isActive) {
                val rec = audioRecord
                if (rec == null || rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    delay(200L)
                    continue
                }
                val readCount = rec.read(shortBuf, 0, shortBuf.size)
                if (readCount > 0 && !isMuted) {
                    var sumAbs = 0L
                    val byteBuf = ByteArray(readCount * 2)
                    for (i in 0 until readCount) {
                        val s = shortBuf[i].toInt()
                        sumAbs += abs(s)
                        byteBuf[i * 2] = (s and 0xFF).toByte()
                        byteBuf[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                    }
                    val avg = (sumAbs / readCount).toInt()
                    localVoiceLevel = (avg / 90).coerceIn(8, 100)
                    // Only transmit when voice energy is above quiet floor to save bandwidth
                    if (avg > 150) {
                        val encoded = Base64.encodeToString(byteBuf, Base64.NO_WRAP)
                        onChunkReady(encoded, seq++)
                    }
                } else {
                    localVoiceLevel = 8
                }
                delay(300L)
            }
        }
    }

    fun playRemoteChunkIfNew(base64Chunk: String, remoteSeq: Int) {
        if (base64Chunk.isBlank() || remoteSeq <= lastPlayedRemoteSeq) return
        lastPlayedRemoteSeq = remoteSeq
        engineScope.launch {
            runCatching {
                val bytes = Base64.decode(base64Chunk, Base64.DEFAULT)
                val track = audioTrack
                if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING && bytes.isNotEmpty()) {
                    track.write(bytes, 0, bytes.size)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun setSpeakerphone(enabled: Boolean) {
        isSpeakerOn = enabled
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enabled) {
                    val speaker = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        am.setCommunicationDevice(speaker)
                    } else {
                        am.isSpeakerphoneOn = true
                    }
                } else {
                    val earpiece = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    }
                    if (earpiece != null) {
                        am.setCommunicationDevice(earpiece)
                    } else {
                        am.clearCommunicationDevice()
                    }
                }
            } else {
                am.isSpeakerphoneOn = enabled
            }
        }
    }

    @Suppress("DEPRECATION")
    fun stopAndRelease() {
        captureJob?.cancel()
        captureJob = null
        runCatching { echoCanceler?.release() }
        echoCanceler = null
        runCatching { noiseSuppressor?.release() }
        noiseSuppressor = null
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am?.clearCommunicationDevice()
            } else {
                am?.isSpeakerphoneOn = false
            }
            am?.mode = AudioManager.MODE_NORMAL
        }
    }

    fun destroy() {
        stopAndRelease()
        engineScope.cancel()
    }
}

@Composable
fun MutualConsentCallOverlay(
    callSession: CallSession,
    currentUid: String,
    language: AppLanguage,
    callRingtoneId: String = MeroRingtoneCatalog.ID_HIMALAYAN_FLUTE,
    callCustomRingtoneUri: String = "",
    hapticEnabled: Boolean = true,
    onAcceptCall: (String) -> Unit,
    onDeclineOrEndCall: (String, String) -> Unit,
    onTransmitAudioChunk: (CallSession, String, Int) -> Unit,
    onShowError: (String) -> Unit
) {
    val colors = LocalMeroColors.current
    val context = LocalContext.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    val isCaller = callSession.callerUid == currentUid
    val partnerName = if (isCaller) callSession.calleeName else callSession.callerName
    val partnerPhoto = if (isCaller) callSession.calleePhoto else callSession.callerPhoto

    var durationSec by remember(callSession.id) { mutableIntStateOf(0) }
    var isMuted by remember(callSession.id) { mutableStateOf(false) }
    var isSpeaker by remember(callSession.id) { mutableStateOf(false) }
    var micLevel by remember(callSession.id) { mutableIntStateOf(12) }

    val audioEngine = remember(callSession.id) { RealTimeCallAudioEngine(context.applicationContext) }

    DisposableEffect(callSession.id) {
        onDispose {
            MeroSoundAndHapticHelper.stopIncomingCallRingtone()
            audioEngine.destroy()
        }
    }

    LaunchedEffect(callSession.id, callSession.status, isCaller, callRingtoneId, callCustomRingtoneUri) {
        if (callSession.status == "RINGING" && !isCaller) {
            MeroSoundAndHapticHelper.startIncomingCallRingtone(
                context = context.applicationContext,
                ringtoneId = callRingtoneId,
                customUriString = callCustomRingtoneUri
            )
            MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled, strong = true)
        } else {
            MeroSoundAndHapticHelper.stopIncomingCallRingtone()
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (!isCaller && callSession.status == "RINGING") {
                onAcceptCall(callSession.id)
            } else if (callSession.status == "ACCEPTED") {
                audioEngine.startAcceptedSession { chunk, seq ->
                    onTransmitAudioChunk(callSession, chunk, seq)
                }
            }
        } else {
            onShowError(
                if (isNe) "अडियो कलका लागि माइक्रोफोन अनुमति आवश्यक छ।"
                else "Microphone permission is required for audio calls."
            )
        }
    }

    // Auto-timeout ringing after 45 seconds -> MISSED
    LaunchedEffect(callSession.id, callSession.status) {
        if (callSession.status == "RINGING") {
            delay(45_000L)
            if (callSession.status == "RINGING") {
                onDeclineOrEndCall(callSession.id, "MISSED")
            }
        }
    }

    // CRITICAL MUTUAL CONSENT RULE:
    // Start microphone capture & playback ONLY when callSession.status == "ACCEPTED"
    LaunchedEffect(callSession.id, callSession.status) {
        if (callSession.status == "ACCEPTED") {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                audioEngine.startAcceptedSession { chunk, seq ->
                    onTransmitAudioChunk(callSession, chunk, seq)
                }
            } else {
                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            while (true) {
                delay(1000L)
                durationSec += 1
                micLevel = audioEngine.localVoiceLevel
            }
        } else {
            audioEngine.stopAndRelease()
        }
    }

    // Play incoming remote audio chunks ONLY when status == "ACCEPTED"
    LaunchedEffect(callSession.status, callSession.callerSeq, callSession.calleeSeq) {
        if (callSession.status == "ACCEPTED") {
            if (isCaller) {
                audioEngine.playRemoteChunkIfNew(callSession.calleeAudioChunk, callSession.calleeSeq)
            } else {
                audioEngine.playRemoteChunkIfNew(callSession.callerAudioChunk, callSession.callerSeq)
            }
        }
    }

    val formattedTimer = remember(durationSec) {
        val mins = (durationSec / 60).toString().padStart(2, '0')
        val secs = (durationSec % 60).toString().padStart(2, '0')
        "$mins:$secs"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(MeroSpacing.lg)
            .testTag("mutual_consent_call_overlay")
    ) {
        MeroCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.lg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MeroSpacing.md)
            ) {
                // Mutual Consent Privacy Badge
                Surface(
                    shape = RoundedCornerShape(MeroRadius.pill),
                    color = colors.surface2,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                        modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isNe) "${s.hdVoiceLabel} · पारस्परिक सहमति" else "${s.hdVoiceLabel} · Mutual Consent",
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                MeroAvatarImage(
                    photoData = partnerPhoto,
                    displayName = partnerName.ifBlank { "Member" },
                    size = 96.dp
                )

                Text(
                    text = partnerName.ifBlank { "Member" },
                    style = MeroType.Title.copy(color = colors.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                when (callSession.status) {
                    "RINGING" -> {
                        Text(
                            text = if (isCaller) {
                                if (isNe) "कल जाँदैछ... ${partnerName} ले स्वीकार गरेपछि मात्र माइक सक्रिय हुनेछ।"
                                else "Ringing... HD audio begins only after $partnerName accepts."
                            } else {
                                if (isNe) "${partnerName} बाट HD अडियो कल आउँदैछ। स्वीकार गरेपछि मात्र आवाज जोडिनेछ।"
                                else "Incoming HD audio call from $partnerName. Audio starts only if you accept."
                            },
                            style = MeroType.Body.copy(color = colors.textMuted),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(MeroSpacing.sm))

                        if (isCaller) {
                            // Caller outgoing controls
                            MeroButton(
                                text = if (isNe) "कल रद्द गर्नुहोस्" else "Cancel Call",
                                leadingIcon = Icons.Filled.CallEnd,
                                onClick = {
                                    MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                    onDeclineOrEndCall(callSession.id, "ENDED")
                                },
                                testTag = "call_cancel_outgoing_button"
                            )
                        } else {
                            // Callee incoming controls: Accept or Decline
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MeroSecondaryButton(
                                    text = if (isNe) "अस्वीकार" else "Decline",
                                    isDanger = true,
                                    onClick = {
                                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                        MeroSoundAndHapticHelper.stopIncomingCallRingtone()
                                        onDeclineOrEndCall(callSession.id, "DECLINED")
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "call_decline_incoming_button"
                                )
                                MeroButton(
                                    text = if (isNe) "स्वीकार गर्नुहोस्" else "Accept Call",
                                    leadingIcon = Icons.Filled.Call,
                                    onClick = {
                                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                        MeroSoundAndHapticHelper.stopIncomingCallRingtone()
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            onAcceptCall(callSession.id)
                                        } else {
                                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "call_accept_incoming_button"
                                )
                            }
                        }
                    }

                    "ACCEPTED" -> {
                        Surface(
                            shape = RoundedCornerShape(MeroRadius.pill),
                            color = colors.success.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (isNe) "HD कल जोडिएको छ · $formattedTimer" else "HD Connected · $formattedTimer",
                                style = MeroType.Heading.copy(color = colors.success),
                                modifier = Modifier.padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
                            )
                        }

                        // Voice activity bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                            modifier = Modifier.height(24.dp)
                        ) {
                            val barFactors = listOf(0.4f, 0.7f, 1.0f, 0.8f, 0.5f)
                            barFactors.forEach { factor ->
                                val h = if (isMuted) 4.dp else ((micLevel * factor) / 4f).coerceIn(4f, 22f).dp
                                Box(
                                    modifier = Modifier
                                        .size(width = 6.dp, height = h)
                                        .clip(CircleShape)
                                        .background(if (isMuted) colors.textMuted else colors.primary)
                                )
                            }
                        }

                        // Active call controls: Mute, Speaker, End Call
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MeroSpacing.sm)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                            ) {
                                MeroIconButton(
                                    icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = if (isMuted) colors.onPrimary else colors.text,
                                    containerColor = if (isMuted) colors.danger else colors.surface2,
                                    onClick = {
                                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                        isMuted = !isMuted
                                        audioEngine.isMuted = isMuted
                                    },
                                    testTag = "call_toggle_mute_button"
                                )
                                Text(
                                    text = if (isMuted) {
                                        if (isNe) "म्युट छ" else "Muted"
                                    } else {
                                        if (isNe) "माइक" else "Mute"
                                    },
                                    style = MeroType.Caption.copy(color = colors.textMuted)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                            ) {
                                MeroIconButton(
                                    icon = Icons.Filled.VolumeUp,
                                    contentDescription = s.speakerLabel,
                                    tint = if (isSpeaker) colors.onPrimary else colors.text,
                                    containerColor = if (isSpeaker) colors.primary else colors.surface2,
                                    onClick = {
                                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                        isSpeaker = !isSpeaker
                                        audioEngine.setSpeakerphone(isSpeaker)
                                    },
                                    testTag = "call_toggle_speaker_button"
                                )
                                Text(
                                    text = s.speakerLabel,
                                    style = MeroType.Caption.copy(
                                        color = if (isSpeaker) colors.primary else colors.textMuted,
                                        fontWeight = if (isSpeaker) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                            ) {
                                MeroIconButton(
                                    icon = Icons.Filled.CallEnd,
                                    contentDescription = "End Call",
                                    tint = colors.onPrimary,
                                    containerColor = colors.danger,
                                    onClick = {
                                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled, strong = true)
                                        onDeclineOrEndCall(callSession.id, "ENDED")
                                    },
                                    testTag = "call_end_active_button"
                                )
                                Text(
                                    text = if (isNe) "कल अन्त्य" else "End Call",
                                    style = MeroType.Caption.copy(color = colors.danger, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
