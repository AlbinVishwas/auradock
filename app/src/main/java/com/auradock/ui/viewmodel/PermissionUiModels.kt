package com.auradock.ui.viewmodel

sealed class PermissionAction {
    data object RequestCalendarPermission : PermissionAction()
    data object OpenNotificationSettings : PermissionAction()
    data object OpenAppSettings : PermissionAction()
}

enum class CalendarPermissionUiState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}
