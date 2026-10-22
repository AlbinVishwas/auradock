package com.auradock.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auradock.ai.TemplateGenerator
import com.auradock.data.CalendarEvent
import com.auradock.data.CalendarRepository
import com.auradock.data.NotificationItem
import com.auradock.data.NotificationRepository
import com.auradock.core.StandbyController
import com.auradock.core.StandbyState as CoreStandbyState
import com.auradock.standby.StandbyState
import com.auradock.standby.StandbyTriggerMode
import com.auradock.ui.template.StandbyTemplate
import com.auradock.ui.template.TemplateDefaults
import com.auradock.ui.template.sanitizedForStandby
import com.auradock.widget.WidgetData
import com.auradock.widget.WidgetManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandbyViewModel @Inject constructor(
    private val controller: StandbyController,
    private val widgetManager: WidgetManager,
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val coreStandbyState: StateFlow<CoreStandbyState> = controller.state

    val widgetDataFlow: SharedFlow<WidgetData> = widgetManager.widgetDataFlow

    private val _currentTemplate = MutableStateFlow(TemplateDefaults.Minimal.sanitizedForStandby())
    val currentTemplate: StateFlow<StandbyTemplate> = _currentTemplate.asStateFlow()
    private val _standbyState = MutableStateFlow(StandbyState.INACTIVE)
    val standbyState: StateFlow<StandbyState> = _standbyState.asStateFlow()
    private val _standbyTriggerMode = MutableStateFlow(StandbyTriggerMode.CHARGING_AND_LANDSCAPE)
    val standbyTriggerMode: StateFlow<StandbyTriggerMode> = _standbyTriggerMode.asStateFlow()
    private val _hasCalendarPermission = MutableStateFlow(calendarRepository.hasCalendarPermission())
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()
    private val _calendarPermissionState = MutableStateFlow(
        if (_hasCalendarPermission.value) CalendarPermissionUiState.GRANTED else CalendarPermissionUiState.DENIED
    )
    val calendarPermissionState: StateFlow<CalendarPermissionUiState> = _calendarPermissionState.asStateFlow()
    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess.asStateFlow()
    private val _permissionActions = MutableSharedFlow<PermissionAction>(extraBufferCapacity = 1)
    val permissionActions: SharedFlow<PermissionAction> = _permissionActions
    val todayEvents: StateFlow<List<CalendarEvent>> = calendarRepository.observeTodayEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val importantNotifications: StateFlow<List<NotificationItem>> = notificationRepository.importantNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    private val _voiceFeedbackText = MutableStateFlow("")
    val voiceFeedbackText: StateFlow<String> = _voiceFeedbackText.asStateFlow()
    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    fun setTemplate(template: StandbyTemplate) {
        _currentTemplate.value = template.sanitizedForStandby()
    }

    fun updateTemplate(update: (StandbyTemplate) -> StandbyTemplate) {
        _currentTemplate.update { existing ->
            update(existing).sanitizedForStandby()
        }
    }

    fun updateStandbyState(state: StandbyState) {
        _standbyState.value = state
    }

    fun updateStandbyTriggerMode(mode: StandbyTriggerMode) {
        _standbyTriggerMode.value = mode
    }

    fun refreshCalendarPermission() {
        val granted = calendarRepository.hasCalendarPermission()
        _hasCalendarPermission.value = granted
        _calendarPermissionState.value = if (granted) {
            CalendarPermissionUiState.GRANTED
        } else {
            CalendarPermissionUiState.DENIED
        }
    }

    fun onCalendarPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _hasCalendarPermission.value = granted
        _calendarPermissionState.value = when {
            granted -> CalendarPermissionUiState.GRANTED
            permanentlyDenied -> CalendarPermissionUiState.PERMANENTLY_DENIED
            else -> CalendarPermissionUiState.DENIED
        }
    }

    fun updateNotificationAccess(granted: Boolean) {
        _hasNotificationAccess.value = granted
    }

    fun requestCalendarPermission() {
        when (_calendarPermissionState.value) {
            CalendarPermissionUiState.GRANTED -> Unit
            CalendarPermissionUiState.DENIED -> _permissionActions.tryEmit(PermissionAction.RequestCalendarPermission)
            CalendarPermissionUiState.PERMANENTLY_DENIED -> _permissionActions.tryEmit(PermissionAction.OpenAppSettings)
        }
    }

    fun openNotificationSettings() {
        _permissionActions.tryEmit(PermissionAction.OpenNotificationSettings)
    }

    fun generateFromPrompt(prompt: String, fromVoice: Boolean = false) {
        viewModelScope.launch {
            if (fromVoice) {
                _voiceState.value = VoiceState.Processing
                _voiceFeedbackText.value = "Creating your layout..."
            }
            _isGenerating.value = true
            try {
                val template = TemplateGenerator.generateTemplateFromPrompt(prompt)
                setTemplate(template)
                if (fromVoice) {
                    _voiceState.value = VoiceState.Success
                    _voiceFeedbackText.value = "Done"
                }
            } catch (_: Throwable) {
                if (fromVoice) {
                    _voiceState.value = VoiceState.Error
                    _voiceFeedbackText.value = "Couldn't understand. Try again."
                }
            } finally {
                _isGenerating.value = false
                if (fromVoice) {
                    delay(1200)
                    resetVoiceState()
                }
            }
        }
    }

    fun onVoiceListeningStarted() {
        _voiceState.value = VoiceState.Listening
        _voiceFeedbackText.value = "Listening..."
        _partialTranscript.value = ""
    }

    fun onVoicePartialTranscript(text: String) {
        if (_voiceState.value == VoiceState.Listening) {
            _partialTranscript.value = text
        }
    }

    fun onVoiceInputError() {
        viewModelScope.launch {
            _voiceState.value = VoiceState.Error
            _voiceFeedbackText.value = "Couldn't understand. Try again."
            delay(1600)
            resetVoiceState()
        }
    }

    private fun resetVoiceState() {
        _voiceState.value = VoiceState.Idle
        _voiceFeedbackText.value = ""
        _partialTranscript.value = ""
    }
}
