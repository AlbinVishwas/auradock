package com.auradock.ui.viewmodel

sealed class PermissionAction {
    data object RequestCalendarPermission : PermissionAction()
    data object OpenNotificationSettings : PermissionAction()
    data object OpenAppSettings : PermissionAction()
    data object RequestNotificationPermission : PermissionAction()
    data object RequestLocationPermission : PermissionAction()
    data object OpenExactAlarmSettings : PermissionAction()
    data object OpenDndAccessSettings : PermissionAction()
    data object OpenOverlaySettings : PermissionAction()
    data object RequestContactsRead : PermissionAction()
    data object RequestContactsWrite : PermissionAction()
    data object PickContact : PermissionAction()
}

enum class CalendarPermissionUiState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}
