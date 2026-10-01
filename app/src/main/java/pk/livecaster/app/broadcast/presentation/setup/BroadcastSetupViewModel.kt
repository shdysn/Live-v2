package pk.livecaster.app.broadcast.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pk.livecaster.app.broadcast.domain.model.Broadcast
import pk.livecaster.app.broadcast.domain.model.BroadcastStatus
import pk.livecaster.app.broadcast.domain.model.PlatformType
import pk.livecaster.app.broadcast.domain.usecase.CreateBroadcastUseCase
import pk.livecaster.app.core.common.Resource
import pk.livecaster.app.core.constants.StreamConstants
import pk.livecaster.app.facebook.domain.repository.FacebookRepository
import pk.livecaster.app.youtube.domain.repository.YouTubeRepository

data class BroadcastSetupUiState(
    val title: String = "LiveCaster Studio Broadcast",
    val description: String = "Broadcasting live with LiveCaster Android Studio",
    val platform: PlatformType = PlatformType.CUSTOM_RTMP,
    val rtmpUrl: String = "rtmp://live.livecaster.pk/live",
    val streamKey: String = "live_stream_key_pk1",
    val resolution: String = "720p",
    val bitrateKbps: Int = StreamConstants.DEFAULT_BITRATE_KBPS,
    val fps: Int = StreamConstants.DEFAULT_FPS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdBroadcastId: Long? = null
)

class BroadcastSetupViewModel(
    private val createBroadcastUseCase: CreateBroadcastUseCase,
    private val facebookRepository: FacebookRepository,
    private val youtubeRepository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BroadcastSetupUiState())
    val uiState: StateFlow<BroadcastSetupUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }

    fun updateDescription(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc)
    }

    fun updatePlatform(platform: PlatformType) {
        val (url, key) = when (platform) {
            PlatformType.FACEBOOK -> Pair("rtmps://live-api-s.facebook.com:443/rtmp/", "fb_live_key_${System.currentTimeMillis() % 100000}")
            PlatformType.YOUTUBE -> Pair("rtmp://a.rtmp.youtube.com/live2", "yt_live_key_${System.currentTimeMillis() % 100000}")
            PlatformType.CUSTOM_RTMP -> Pair("rtmp://live.livecaster.pk/live", "live_key_custom")
            PlatformType.MULTI_DESTINATION -> Pair("rtmp://relay.livecaster.pk/multi", "multi_dest_key")
        }
        _uiState.value = _uiState.value.copy(
            platform = platform,
            rtmpUrl = url,
            streamKey = key
        )
    }

    fun updateRtmpUrl(url: String) {
        _uiState.value = _uiState.value.copy(rtmpUrl = url)
    }

    fun updateStreamKey(key: String) {
        _uiState.value = _uiState.value.copy(streamKey = key)
    }

    fun updateQuality(resolution: String, bitrate: Int, fps: Int) {
        _uiState.value = _uiState.value.copy(
            resolution = resolution,
            bitrateKbps = bitrate,
            fps = fps
        )
    }

    fun createAndStartBroadcast(onSuccess: (broadcastId: Long) -> Unit) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true, errorMessage = null)

            val broadcast = Broadcast(
                title = current.title.trim(),
                description = current.description.trim(),
                rtmpUrl = current.rtmpUrl.trim(),
                streamKey = current.streamKey.trim(),
                platform = current.platform,
                status = BroadcastStatus.DRAFT,
                resolution = current.resolution,
                bitrateKbps = current.bitrateKbps,
                fps = current.fps
            )

            when (val result = createBroadcastUseCase(broadcast)) {
                is Resource.Success -> {
                    _uiState.value = current.copy(isLoading = false, createdBroadcastId = result.data)
                    onSuccess(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = current.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
