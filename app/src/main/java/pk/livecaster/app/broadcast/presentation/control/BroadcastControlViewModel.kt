package pk.livecaster.app.broadcast.presentation.control

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pk.livecaster.app.broadcast.domain.model.Broadcast
import pk.livecaster.app.broadcast.domain.model.BroadcastStatus
import pk.livecaster.app.broadcast.domain.repository.BroadcastRepository
import pk.livecaster.app.broadcast.domain.usecase.GetBroadcastsUseCase
import pk.livecaster.app.broadcast.domain.usecase.UpdateBroadcastStatusUseCase
import pk.livecaster.app.streaming.encoder.AudioEncoderConfig
import pk.livecaster.app.streaming.encoder.VideoEncoderConfig
import pk.livecaster.app.streaming.publisher.RtmpPublisher
import pk.livecaster.app.streaming.service.LiveStreamingService
import pk.livecaster.app.streaming.state.StreamStatus
import pk.livecaster.app.streaming.state.StreamTelemetry
import kotlin.random.Random

data class LiveChatMessage(
    val id: String,
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BroadcastControlUiState(
    val broadcast: Broadcast? = null,
    val telemetry: StreamTelemetry = StreamTelemetry(),
    val chatMessages: List<LiveChatMessage> = emptyList(),
    val isLowerThirdVisible: Boolean = true,
    val audioVuLevel: Float = 0.65f, // 0.0 to 1.0
    val showEndConfirmDialog: Boolean = false
)

class BroadcastControlViewModel(
    private val broadcastId: Long,
    private val getBroadcastsUseCase: GetBroadcastsUseCase,
    private val updateStatusUseCase: UpdateBroadcastStatusUseCase,
    private val broadcastRepository: BroadcastRepository,
    private val publisher: RtmpPublisher,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BroadcastControlUiState())
    val uiState: StateFlow<BroadcastControlUiState> = _uiState.asStateFlow()

    private var vuMeterJob: Job? = null
    private var chatSimulationJob: Job? = null

    init {
        observeBroadcast()
        observeTelemetry()
        startAudioVuSimulation()
    }

    private fun observeBroadcast() {
        viewModelScope.launch {
            getBroadcastsUseCase.getById(broadcastId).collect { bc ->
                _uiState.value = _uiState.value.copy(broadcast = bc)
            }
        }
    }

    private fun observeTelemetry() {
        viewModelScope.launch {
            publisher.telemetry.collect { tele ->
                _uiState.value = _uiState.value.copy(telemetry = tele)
                // Persist telemetry updates periodically
                if (tele.status == StreamStatus.LIVE && tele.durationSeconds % 5 == 0L) {
                    broadcastRepository.updateTelemetry(broadcastId, tele.durationSeconds, tele.currentViewers)
                }
            }
        }
    }

    fun startLiveStream() {
        val bc = _uiState.value.broadcast ?: return
        val videoConfig = VideoEncoderConfig(
            bitrateKbps = bc.bitrateKbps,
            frameRate = bc.fps
        )
        val audioConfig = AudioEncoderConfig()

        LiveStreamingService.startService(appContext, bc.title)
        publisher.startPublishing(bc.rtmpUrl, bc.streamKey, videoConfig, audioConfig)

        viewModelScope.launch {
            updateStatusUseCase(broadcastId, BroadcastStatus.LIVE)
        }
        startChatSimulation()
    }

    fun stopLiveStream() {
        publisher.stopPublishing()
        LiveStreamingService.stopService(appContext)

        viewModelScope.launch {
            updateStatusUseCase(broadcastId, BroadcastStatus.ENDED)
        }
        chatSimulationJob?.cancel()
    }

    fun toggleMic(): Boolean = publisher.toggleMicMute()
    fun toggleTorch(): Boolean = publisher.toggleTorch()
    fun toggleCamera(): Boolean = publisher.toggleCameraFacing()

    fun toggleLowerThird() {
        _uiState.value = _uiState.value.copy(isLowerThirdVisible = !_uiState.value.isLowerThirdVisible)
    }

    fun promptEndConfirmation(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEndConfirmDialog = show)
    }

    private fun startAudioVuSimulation() {
        vuMeterJob?.cancel()
        vuMeterJob = viewModelScope.launch {
            while (true) {
                delay(120)
                if (!_uiState.value.telemetry.isMicMuted && _uiState.value.telemetry.status == StreamStatus.LIVE) {
                    val level = Random.nextFloat().coerceIn(0.2f, 0.95f)
                    _uiState.value = _uiState.value.copy(audioVuLevel = level)
                } else {
                    _uiState.value = _uiState.value.copy(audioVuLevel = 0.05f)
                }
            }
        }
    }

    private fun startChatSimulation() {
        chatSimulationJob?.cancel()
        chatSimulationJob = viewModelScope.launch {
            val sampleComments = listOf(
                "Tariq" to "Audio and video crystal clear from Lahore! 🇵🇰",
                "Ayesha" to "Great quality broadcast, FPS is solid",
                "Bilal" to "Mashallah, live stream looking super professional!",
                "Hamza" to "Hello from Karachi, watching the stream",
                "Zainab" to "What encoder bitrate are you running?",
                "Usman" to "Stream quality is top notch!"
            )
            var index = 0
            while (true) {
                delay(4000)
                val (sender, text) = sampleComments[index % sampleComments.size]
                val msg = LiveChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    sender = sender,
                    message = text
                )
                val updated = (_uiState.value.chatMessages + msg).takeLast(6)
                _uiState.value = _uiState.value.copy(chatMessages = updated)
                index++
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        vuMeterJob?.cancel()
        chatSimulationJob?.cancel()
    }
}
