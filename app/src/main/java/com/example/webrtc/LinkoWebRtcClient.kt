package com.example.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpParameters
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Production-grade WebRTC Client for Linko HD Audio & Video Calling.
 *
 * Features:
 * 1. **STUN & Pluggable TURN Servers**:
 *    - Pre-configured with Google's global STUN cluster (`stun.l.google.com:19302` .. `stun4`).
 *    - First-class [TurnServerConfig] support for adding custom UDP/TCP/TLS TURN relays.
 * 2. **HD Audio & Hardware/Software Noise Cancellation**:
 *    - Uses [JavaAudioDeviceModule] at 48 kHz Fullband sample rate with hardware Acoustic Echo
 *      Canceler (AEC) and Hardware Noise Suppressor (NS) enabled when supported by the chipset.
 *    - Enforces WebRTC DSP audio constraints (`googEchoCancellation`, `googNoiseSuppression`,
 *      `googAutoGainControl`, `googHighpassFilter`, `googTypingNoiseDetection`) and tunes the
 *      Opus codec in SDP (`maxaveragebitrate=64000; useinbandfec=1; usedtx=1`).
 * 3. **Adaptive High-Definition Video (720p / 1080p @ 30fps)**:
 *    - Hardware-accelerated VP8/VP9/H.264 encoding & decoding via [EglBase].
 *    - Captures at 1280x720 @ 30fps (or 1920x1080 @ 30fps when Full HD is toggled) and configures
 *      [RtpParameters.DegradationPreference.BALANCED] with adaptive bitrate scaling.
 */
class LinkoWebRtcClient(
    private val context: Context,
    private val customTurnServers: List<TurnServerConfig> = emptyList(),
    private val listener: WebRtcEventListener
) {

    companion object {
        private const val TAG = "LinkoWebRtcClient"
        private const val LOCAL_AUDIO_TRACK_ID = "linko_local_audio_track"
        private const val LOCAL_VIDEO_TRACK_ID = "linko_local_video_track"
        private const val LOCAL_MEDIA_STREAM_ID = "linko_local_media_stream"

        const val HD_720P_WIDTH = 1280
        const val HD_720P_HEIGHT = 720
        const val FHD_1080P_WIDTH = 1920
        const val FHD_1080P_HEIGHT = 1080
        const val TARGET_FPS = 30

        private const val MAX_VIDEO_BITRATE_720P_BPS = 2_500_000   // 2.5 Mbps for 720p30
        private const val MAX_VIDEO_BITRATE_1080P_BPS = 4_500_000  // 4.5 Mbps for 1080p30
        private const val MIN_VIDEO_BITRATE_BPS = 300_000          // 300 kbps floor on weak networks
        private const val OPUS_HD_BITRATE_BPS = 64_000             // 64 kbps Fullband HD Opus

        val DEFAULT_GOOGLE_STUN_URLS = listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302",
            "stun:stun3.l.google.com:19302",
            "stun:stun4.l.google.com:19302"
        )
    }

    /**
     * Configuration model for plugging in a TURN server (e.g. Coturn, Twilio, Cloudflare, Metered).
     */
    data class TurnServerConfig(
        val urls: List<String>,
        val username: String,
        val credential: String
    )

    interface WebRtcEventListener {
        fun onLocalIceCandidateGenerated(candidate: IceCandidate)
        fun onRemoteVideoTrackAvailable(videoTrack: VideoTrack)
        fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState)
        fun onWebRtcError(message: String)
    }

    val eglBase: EglBase = EglBase.create()
    val eglBaseContext: EglBase.Context
        get() = eglBase.eglBaseContext

    private val audioManager: AudioManager? =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var peerConnection: PeerConnection? = null

    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoSender: RtpSender? = null
    private var audioSender: RtpSender? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var latestRemoteVideoTrack: VideoTrack? = null

    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false
    private var isFrontCameraActive = true
    private var is1080pMode = false
    private var isDisposed = false

    val isHardwareAecSupported: Boolean
        get() = JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported()

    val isHardwareNsSupported: Boolean
        get() = JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported()

    init {
        initializePeerConnectionFactory()
    }

    /**
     * Step 1: Initialize WebRTC [PeerConnectionFactory] with [JavaAudioDeviceModule] (Hardware + Software
     * AEC/NS at 48 kHz) and Hardware-Accelerated Video Codecs.
     */
    private fun initializePeerConnectionFactory() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )

            val adm = JavaAudioDeviceModule.builder(context.applicationContext)
                .setUseHardwareAcousticEchoCanceler(isHardwareAecSupported)
                .setUseHardwareNoiseSuppressor(isHardwareNsSupported)
                .setSampleRate(48000)
                .setUseStereoInput(false)
                .setUseStereoOutput(false)
                .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                    override fun onWebRtcAudioRecordInitError(errorMessage: String?) {
                        Log.w(TAG, "AudioRecord init warning: $errorMessage")
                    }

                    override fun onWebRtcAudioRecordStartError(
                        errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                        errorMessage: String?
                    ) {
                        Log.w(TAG, "AudioRecord start warning ($errorCode): $errorMessage")
                    }

                    override fun onWebRtcAudioRecordError(errorMessage: String?) {
                        Log.w(TAG, "AudioRecord runtime warning: $errorMessage")
                    }
                })
                .createAudioDeviceModule()

            audioDeviceModule = adm

            val videoEncoderFactory = DefaultVideoEncoderFactory(
                eglBaseContext,
                /* enableIntelVp8Encoder = */ true,
                /* enableH264HighProfile = */ true
            )
            val videoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()

            Log.d(
                TAG,
                "WebRTC Factory initialized (HW AEC=$isHardwareAecSupported, HW NS=$isHardwareNsSupported, 48kHz HD Audio)"
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize PeerConnectionFactory: ${t.message}", t)
            listener.onWebRtcError("WebRTC initialization warning: ${t.localizedMessage}")
        }
    }

    /**
     * Builds the list of ICE Servers combining Google's public STUN cluster and any configured
     * TURN servers for symmetric NAT / firewall traversal.
     */
    fun buildIceServers(additionalTurnServers: List<TurnServerConfig> = emptyList()): List<PeerConnection.IceServer> {
        val iceServers = mutableListOf<PeerConnection.IceServer>()

        // 1. Standard Public Google STUN Servers
        DEFAULT_GOOGLE_STUN_URLS.forEach { stunUrl ->
            iceServers.add(PeerConnection.IceServer.builder(stunUrl).createIceServer())
        }

        // 2. Pluggable TURN Servers (UDP, TCP, and TLS 443)
        (customTurnServers + additionalTurnServers).forEach { turn ->
            if (turn.urls.isNotEmpty()) {
                val builder = PeerConnection.IceServer.builder(turn.urls)
                if (turn.username.isNotBlank()) {
                    builder.setUsername(turn.username)
                }
                if (turn.credential.isNotBlank()) {
                    builder.setPassword(turn.credential)
                }
                iceServers.add(builder.createIceServer())
            }
        }

        return iceServers
    }

    /**
     * Step 2: Create the [PeerConnection] with Unified Plan SDP semantics, Continual Gathering,
     * HD Audio constraints (AEC, NS, AGC), and optional HD Video capture (720p/1080p @ 30fps).
     */
    fun startLocalMediaAndCreatePeerConnection(
        isVideoCall: Boolean,
        startIn1080p: Boolean = false
    ) {
        val factory = peerConnectionFactory ?: return
        is1080pMode = startIn1080p
        isRemoteDescriptionSet = false
        pendingRemoteCandidates.clear()

        configureAudioManagerForCall(enableSpeaker = isVideoCall)

        val rtcConfig = PeerConnection.RTCConfiguration(buildIceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            keyType = PeerConnection.KeyType.ECDSA
        }

        peerConnection = factory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                    Log.d(TAG, "Signaling state changed: $newState")
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "ICE Connection state changed: $newState")
                    if (newState != null) {
                        listener.onIceConnectionStateChanged(newState)
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}

                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                    Log.d(TAG, "ICE Gathering state: $newState")
                }

                override fun onIceCandidate(candidate: IceCandidate?) {
                    if (candidate != null) {
                        listener.onLocalIceCandidateGenerated(candidate)
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

                override fun onAddStream(stream: MediaStream?) {
                    val remoteTrack = stream?.videoTracks?.firstOrNull() ?: return
                    attachRemoteVideoTrack(remoteTrack)
                }

                override fun onRemoveStream(stream: MediaStream?) {}

                override fun onDataChannel(dataChannel: DataChannel?) {}

                override fun onRenegotiationNeeded() {}

                override fun onAddTrack(
                    receiver: RtpReceiver?,
                    mediaStreams: Array<out MediaStream>?
                ) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        attachRemoteVideoTrack(track)
                    }
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    if (track is VideoTrack) {
                        attachRemoteVideoTrack(track)
                    }
                }
            }
        )

        // Start HD Audio Track with AEC, NS, AGC, and High-Pass Filter constraints
        setupHdAudioTrack(factory)

        // Start HD Video Track (720p or 1080p @ 30fps) if this is a video call
        if (isVideoCall) {
            setupHdVideoTrack(factory, use1080p = startIn1080p)
        }
    }

    /**
     * Enforces High-Definition (HD) Audio and explicitly activates hardware/software
     * Acoustic Echo Cancellation (AEC), Noise Suppression (NS), Auto Gain Control (AGC),
     * and High-Pass Filtering.
     */
    fun buildHdAudioConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation2", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googDAEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl2", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression2", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
        }
    }

    private fun setupHdAudioTrack(factory: PeerConnectionFactory) {
        try {
            val audioConstraints = buildHdAudioConstraints()
            audioSource = factory.createAudioSource(audioConstraints)
            localAudioTrack = factory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource).apply {
                setEnabled(true)
                setVolume(1.0)
            }
            localAudioTrack?.let { track ->
                audioSender = peerConnection?.addTrack(track, listOf(LOCAL_MEDIA_STREAM_ID))
                configureAudioSenderBitrate(audioSender)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not create local audio track: ${e.message}")
        }
    }

    private fun configureAudioSenderBitrate(sender: RtpSender?) {
        val rtpSender = sender ?: return
        try {
            val params = rtpSender.parameters
            if (params.encodings.isNotEmpty()) {
                params.encodings[0].maxBitrateBps = OPUS_HD_BITRATE_BPS
                rtpSender.parameters = params
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio bitrate tuning skipped: ${e.message}")
        }
    }

    /**
     * Initializes Camera2 (or Camera1 fallback) video capture at HD 720p (1280x720) or
     * Full HD 1080p (1920x1080) @ 30fps and configures adaptive bandwidth scaling.
     */
    private fun setupHdVideoTrack(factory: PeerConnectionFactory, use1080p: Boolean) {
        try {
            val capturer = createCameraCapturer(preferFrontCamera = true)
            videoCapturer = capturer
            isFrontCameraActive = true

            if (capturer != null) {
                surfaceTextureHelper = SurfaceTextureHelper.create("LinkoCameraCaptureThread", eglBaseContext)
                videoSource = factory.createVideoSource(capturer.isScreencast)
                capturer.initialize(surfaceTextureHelper, context.applicationContext, videoSource?.capturerObserver)

                val width = if (use1080p) FHD_1080P_WIDTH else HD_720P_WIDTH
                val height = if (use1080p) FHD_1080P_HEIGHT else HD_720P_HEIGHT
                try {
                    capturer.startCapture(width, height, TARGET_FPS)
                } catch (e: Exception) {
                    Log.w(TAG, "1080p/720p capture fallback to 1280x720@30: ${e.message}")
                    try {
                        capturer.startCapture(HD_720P_WIDTH, HD_720P_HEIGHT, TARGET_FPS)
                    } catch (_: Exception) {}
                }

                localVideoTrack = factory.createVideoTrack(LOCAL_VIDEO_TRACK_ID, videoSource).apply {
                    setEnabled(true)
                }

                localRenderer?.let { renderer ->
                    localVideoTrack?.addSink(renderer)
                }

                localVideoTrack?.let { track ->
                    videoSender = peerConnection?.addTrack(track, listOf(LOCAL_MEDIA_STREAM_ID))
                    configureAdaptiveVideoBitrate(videoSender, use1080p = use1080p)
                }
            } else {
                Log.w(TAG, "No hardware camera enumerator available on this device/emulator")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Video track initialization fallback: ${e.message}")
        }
    }

    /**
     * Configures [RtpParameters] on the video sender for HD streaming while allowing WebRTC's
     * bandwidth estimator to automatically scale resolution/framerate (`BALANCED`) on slower networks.
     */
    private fun configureAdaptiveVideoBitrate(sender: RtpSender?, use1080p: Boolean) {
        val rtpSender = sender ?: return
        try {
            val parameters = rtpSender.parameters
            parameters.degradationPreference = RtpParameters.DegradationPreference.BALANCED
            if (parameters.encodings.isNotEmpty()) {
                val encoding = parameters.encodings[0]
                encoding.maxBitrateBps = if (use1080p) MAX_VIDEO_BITRATE_1080P_BPS else MAX_VIDEO_BITRATE_720P_BPS
                encoding.minBitrateBps = MIN_VIDEO_BITRATE_BPS
                encoding.maxFramerate = TARGET_FPS
            }
            rtpSender.parameters = parameters
        } catch (e: Exception) {
            Log.w(TAG, "Adaptive video bitrate setup warning: ${e.message}")
        }
    }

    private fun createCameraCapturer(preferFrontCamera: Boolean): CameraVideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true)
        }
        val deviceNames = enumerator.deviceNames
        if (deviceNames.isEmpty()) return null

        // First pass: preferred facing direction
        for (name in deviceNames) {
            val matchesPreferred = if (preferFrontCamera) {
                enumerator.isFrontFacing(name)
            } else {
                enumerator.isBackFacing(name)
            }
            if (matchesPreferred) {
                val capturer = enumerator.createCapturer(name, null)
                if (capturer != null) return capturer
            }
        }

        // Second pass: any available camera
        for (name in deviceNames) {
            val capturer = enumerator.createCapturer(name, null)
            if (capturer != null) return capturer
        }
        return null
    }

    /**
     * Attaches the Compose [SurfaceViewRenderer] for the local camera preview.
     */
    fun bindLocalRenderer(renderer: SurfaceViewRenderer) {
        try {
            if (localRenderer !== renderer) {
                localRenderer?.let { old -> localVideoTrack?.removeSink(old) }
                localRenderer = renderer
                localVideoTrack?.addSink(renderer)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error binding local renderer: ${e.message}")
        }
    }

    /**
     * Attaches the Compose [SurfaceViewRenderer] for the remote peer's video stream.
     */
    fun bindRemoteRenderer(renderer: SurfaceViewRenderer) {
        try {
            if (remoteRenderer !== renderer) {
                remoteRenderer?.let { old -> latestRemoteVideoTrack?.removeSink(old) }
                remoteRenderer = renderer
                latestRemoteVideoTrack?.addSink(renderer)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error binding remote renderer: ${e.message}")
        }
    }

    private fun attachRemoteVideoTrack(videoTrack: VideoTrack) {
        latestRemoteVideoTrack = videoTrack
        videoTrack.setEnabled(true)
        remoteRenderer?.let { renderer ->
            videoTrack.addSink(renderer)
        }
        listener.onRemoteVideoTrackAvailable(videoTrack)
    }

    /**
     * Creates an SDP Offer configured for HD Opus Audio and (when applicable) HD Video.
     */
    fun createOffer(
        isVideoCall: Boolean,
        onOfferCreated: (SessionDescription) -> Unit
    ) {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false")
            )
        }

        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc == null) return
                val hdEnhancedSdp = SessionDescription(desc.type, enhanceSdpForHdAudio(desc.description))
                pc.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        onOfferCreated(hdEnhancedSdp)
                    }
                }, hdEnhancedSdp)
            }

            override fun onCreateFailure(error: String?) {
                listener.onWebRtcError("Failed to create SDP Offer: $error")
            }
        }, constraints)
    }

    /**
     * Sets the remote caller's SDP Offer and generates the local SDP Answer.
     */
    fun handleRemoteOfferAndCreateAnswer(
        remoteOffer: SessionDescription,
        isVideoCall: Boolean,
        onAnswerCreated: (SessionDescription) -> Unit
    ) {
        val pc = peerConnection ?: return
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainPendingIceCandidates()

                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(
                        MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false")
                    )
                }

                pc.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        if (desc == null) return
                        val hdEnhancedSdp = SessionDescription(desc.type, enhanceSdpForHdAudio(desc.description))
                        pc.setLocalDescription(object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                onAnswerCreated(hdEnhancedSdp)
                            }
                        }, hdEnhancedSdp)
                    }

                    override fun onCreateFailure(error: String?) {
                        listener.onWebRtcError("Failed to create SDP Answer: $error")
                    }
                }, constraints)
            }

            override fun onSetFailure(error: String?) {
                listener.onWebRtcError("Failed to set remote SDP Offer: $error")
            }
        }, remoteOffer)
    }

    /**
     * Applies the remote peer's SDP Answer once received from Firebase signaling.
     */
    fun handleRemoteAnswer(remoteAnswer: SessionDescription) {
        val pc = peerConnection ?: return
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainPendingIceCandidates()
            }

            override fun onSetFailure(error: String?) {
                listener.onWebRtcError("Failed to set remote SDP Answer: $error")
            }
        }, remoteAnswer)
    }

    /**
     * Adds a remote Trickle ICE Candidate (queuing it safely if remote SDP is not yet applied).
     */
    fun addRemoteIceCandidate(candidate: IceCandidate) {
        val pc = peerConnection ?: return
        if (!isRemoteDescriptionSet) {
            synchronized(pendingRemoteCandidates) {
                pendingRemoteCandidates.add(candidate)
            }
        } else {
            pc.addIceCandidate(candidate)
        }
    }

    private fun drainPendingIceCandidates() {
        val pc = peerConnection ?: return
        synchronized(pendingRemoteCandidates) {
            pendingRemoteCandidates.forEach { pc.addIceCandidate(it) }
            pendingRemoteCandidates.clear()
        }
    }

    /**
     * Enriches the Opus fmtp line in the SDP to enforce 48 kHz Fullband HD voice clarity,
     * Forward Error Correction (`useinbandfec=1`), and Discontinuous Transmission (`usedtx=1`).
     */
    fun enhanceSdpForHdAudio(rawSdp: String): String {
        if (!rawSdp.contains("opus/48000", ignoreCase = true)) return rawSdp
        return rawSdp.lines().joinToString("\r\n") { line ->
            if (line.startsWith("a=fmtp:") && line.contains("minptime=")) {
                val base = line.trimEnd(';')
                "$base;maxplaybackrate=48000;sprop-maxcapturerate=48000;maxaveragebitrate=64000;useinbandfec=1;usedtx=1;cbr=0"
            } else {
                line
            }
        }
    }

    /**
     * Controls microphone mute state.
     */
    fun setMicrophoneMuted(muted: Boolean) {
        try {
            localAudioTrack?.setEnabled(!muted)
            audioDeviceModule?.setMicrophoneMute(muted)
        } catch (e: Exception) {
            Log.w(TAG, "Error toggling mic mute: ${e.message}")
        }
    }

    /**
     * Enables or disables local camera transmission.
     */
    fun setVideoEnabled(enabled: Boolean) {
        try {
            localVideoTrack?.setEnabled(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Error toggling video enabled: ${e.message}")
        }
    }

    /**
     * Switches between Front and Back cameras smoothly.
     */
    fun switchCamera(onSwitched: (isFrontCamera: Boolean) -> Unit = {}) {
        val capturer = videoCapturer
        if (capturer == null) {
            isFrontCameraActive = !isFrontCameraActive
            onSwitched(isFrontCameraActive)
            return
        }
        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                isFrontCameraActive = isFrontCamera
                onSwitched(isFrontCamera)
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.w(TAG, "Camera switch warning: $errorDescription")
                onSwitched(isFrontCameraActive)
            }
        })
    }

    /**
     * Dynamically switches capture format and encoder bitrate between HD 720p (1280x720)
     * and Full HD 1080p (1920x1080) @ 30fps without dropping the call.
     */
    fun setHighDefinitionResolution(enable1080p: Boolean) {
        is1080pMode = enable1080p
        val width = if (enable1080p) FHD_1080P_WIDTH else HD_720P_WIDTH
        val height = if (enable1080p) FHD_1080P_HEIGHT else HD_720P_HEIGHT
        try {
            videoCapturer?.changeCaptureFormat(width, height, TARGET_FPS)
            configureAdaptiveVideoBitrate(videoSender, use1080p = enable1080p)
        } catch (e: Exception) {
            Log.w(TAG, "Resolution change warning: ${e.message}")
        }
    }

    /**
     * Routes audio output between Speakerphone and Earpiece.
     */
    @Suppress("DEPRECATION")
    fun setSpeakerphoneEnabled(enabled: Boolean) {
        val am = audioManager ?: return
        try {
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val targetType = if (enabled) {
                    android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
                val device = am.availableCommunicationDevices.firstOrNull { it.type == targetType }
                if (device != null) {
                    am.setCommunicationDevice(device)
                } else {
                    am.isSpeakerphoneOn = enabled
                }
            } else {
                am.isSpeakerphoneOn = enabled
            }
        } catch (e: Exception) {
            Log.w(TAG, "Speakerphone toggle warning: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun configureAudioManagerForCall(enableSpeaker: Boolean) {
        val am = audioManager ?: return
        try {
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener {}
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            setSpeakerphoneEnabled(enableSpeaker)
        } catch (e: Exception) {
            Log.w(TAG, "AudioManager setup warning: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun releaseAudioManager() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.clearCommunicationDevice()
            }
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_NORMAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                am.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }

    /**
     * Releases all WebRTC native resources, camera capturers, audio modules, and EGL contexts.
     */
    fun release() {
        if (isDisposed) return
        isDisposed = true
        try {
            localRenderer?.let { localVideoTrack?.removeSink(it) }
            remoteRenderer?.let { latestRemoteVideoTrack?.removeSink(it) }
            localRenderer = null
            remoteRenderer = null
            latestRemoteVideoTrack = null

            try {
                videoCapturer?.stopCapture()
            } catch (_: Exception) {}
            videoCapturer?.dispose()
            videoCapturer = null

            videoSource?.dispose()
            videoSource = null

            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            audioSource?.dispose()
            audioSource = null

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null

            audioDeviceModule?.release()
            audioDeviceModule = null

            peerConnectionFactory?.dispose()
            peerConnectionFactory = null

            eglBase.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WebRTC client: ${e.message}")
        } finally {
            releaseAudioManager()
        }
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}
