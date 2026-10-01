package pk.livecaster.app.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pk.livecaster.app.core.security.SecureTokenStorage
import pk.livecaster.app.facebook.domain.repository.FacebookRepository
import pk.livecaster.app.youtube.domain.repository.YouTubeRepository

data class ConnectAccountsUiState(
    // Facebook section state
    val isFacebookLoggedIn: Boolean = false,
    val selectedFacebookPage: String = "Shahid Live TV",
    val availableFacebookPages: List<String> = listOf(
        "Shahid Live TV",
        "LiveCaster Pakistan Official",
        "Sports HD Live Stream",
        "Urdu News HD"
    ),
    val isFacebookConnected: Boolean = true,
    val facebookConnectedName: String = "Shahid Live TV",
    val facebookPermissions: List<String> = listOf(
        "View managed Pages",
        "Create live broadcasts",
        "Read Page engagement"
    ),

    // YouTube section state
    val isGoogleLoggedIn: Boolean = false,
    val selectedYouTubeChannel: String = "Shahid Live TV",
    val availableYouTubeChannels: List<String> = listOf(
        "Shahid Live TV",
        "LiveCaster Pakistan Stream Studio",
        "Urdu News 24/7 Live Stream",
        "Shahid Tech Live"
    ),
    val isYouTubeConnected: Boolean = true,
    val youtubeConnectedName: String = "Shahid Live TV",
    val youTubePermissions: List<String> = listOf(
        "View YouTube Channel",
        "Create and manage live broadcasts",
        "View Live status"
    ),

    val isLoading: Boolean = false,
    val message: String? = null
)

class ConnectAccountsViewModel(
    private val facebookRepository: FacebookRepository,
    private val youtubeRepository: YouTubeRepository,
    private val tokenStorage: SecureTokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectAccountsUiState())
    val uiState: StateFlow<ConnectAccountsUiState> = _uiState.asStateFlow()

    init {
        // Ensure Shahid Live TV exists in repositories on startup
        viewModelScope.launch {
            facebookRepository.linkPage(
                pageName = "Shahid Live TV",
                pageId = "fb_page_shahid_live",
                pageToken = "EAAB_shahid_live_token"
            )
            youtubeRepository.linkChannel(
                title = "Shahid Live TV",
                channelId = "UC_shahid_live_tv",
                customUrl = "@ShahidLiveTV"
            )
        }
    }

    fun continueWithFacebook() {
        _uiState.value = _uiState.value.copy(
            isFacebookLoggedIn = true,
            message = "Facebook account authenticated. Select Page to link."
        )
    }

    fun selectFacebookPage(page: String) {
        _uiState.value = _uiState.value.copy(selectedFacebookPage = page)
    }

    fun cancelFacebook() {
        _uiState.value = _uiState.value.copy(
            isFacebookLoggedIn = false
        )
    }

    fun connectFacebookPage() {
        viewModelScope.launch {
            val pageName = _uiState.value.selectedFacebookPage
            facebookRepository.linkPage(
                pageName = pageName,
                pageId = "fb_page_${pageName.replace(" ", "_").lowercase()}",
                pageToken = "EAAB_${System.currentTimeMillis()}"
            )
            tokenStorage.saveFacebookToken("fb_auth_token_${System.currentTimeMillis()}")
            _uiState.value = _uiState.value.copy(
                isFacebookConnected = true,
                facebookConnectedName = pageName,
                isFacebookLoggedIn = false,
                message = "Facebook: Connected to $pageName (Live)"
            )
        }
    }

    fun disconnectFacebook() {
        _uiState.value = _uiState.value.copy(
            isFacebookConnected = false,
            isFacebookLoggedIn = false,
            message = "Facebook Page disconnected"
        )
    }

    fun continueWithGoogle() {
        _uiState.value = _uiState.value.copy(
            isGoogleLoggedIn = true,
            message = "Google account authenticated. Select Channel to link."
        )
    }

    fun selectYouTubeChannel(channel: String) {
        _uiState.value = _uiState.value.copy(selectedYouTubeChannel = channel)
    }

    fun cancelGoogle() {
        _uiState.value = _uiState.value.copy(
            isGoogleLoggedIn = false
        )
    }

    fun connectYouTubeChannel() {
        viewModelScope.launch {
            val channelName = _uiState.value.selectedYouTubeChannel
            youtubeRepository.linkChannel(
                title = channelName,
                channelId = "UC_${channelName.replace(" ", "_").lowercase()}",
                customUrl = "@${channelName.replace(" ", "")}"
            )
            tokenStorage.saveYouTubeToken("yt_auth_token_${System.currentTimeMillis()}")
            _uiState.value = _uiState.value.copy(
                isYouTubeConnected = true,
                youtubeConnectedName = channelName,
                isGoogleLoggedIn = false,
                message = "YouTube: Connected to $channelName (Live)"
            )
        }
    }

    fun disconnectYouTube() {
        _uiState.value = _uiState.value.copy(
            isYouTubeConnected = false,
            isGoogleLoggedIn = false,
            message = "YouTube Channel disconnected"
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
