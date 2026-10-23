package com.auradock.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auradock.data.CalendarEvent
import com.auradock.data.CalendarRepository
import com.auradock.data.NotificationItem
import com.auradock.data.NotificationRepository
import com.auradock.standby.StandbyState
import com.auradock.standby.StandbyTriggerManager
import com.auradock.time.MinuteClock
import com.auradock.ui.template.StandbyTemplate
import com.auradock.ui.template.TemplateDefaults
import com.auradock.ui.template.sanitizedForStandby
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class StandbyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository,
    minuteClock: MinuteClock
) : ViewModel() {

    /** Current time, refreshed at the start of every minute. Shared by the clock, preview and dashboard date. */
    val currentTimeMillis: StateFlow<Long> = minuteClock.minutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), minuteClock.currentMillis())

    private val _currentTemplate = MutableStateFlow(TemplateDefaults.Minimal.sanitizedForStandby())
    val currentTemplate: StateFlow<StandbyTemplate> = _currentTemplate.asStateFlow()

    // Owned by the ViewModel (not the Activity) so a rotation, which destroys and recreates the
    // Activity, does not stop and restart it. It is stopped in onCleared().
    private val standbyTriggerManager = StandbyTriggerManager(context).also { it.start() }
    val standbyState: StateFlow<StandbyState> = standbyTriggerManager.standbyState

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

    fun setTemplate(template: StandbyTemplate) {
        _currentTemplate.value = template.sanitizedForStandby()
    }

    fun updateTemplate(update: (StandbyTemplate) -> StandbyTemplate) {
        _currentTemplate.update { existing ->
            update(existing).sanitizedForStandby()
        }
    }

    override fun onCleared() {
        standbyTriggerManager.stop()
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
}
