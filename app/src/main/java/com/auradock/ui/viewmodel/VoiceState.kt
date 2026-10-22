package com.auradock.ui.viewmodel

sealed class VoiceState {
    data object Idle : VoiceState()
    data object Listening : VoiceState()
    data object Processing : VoiceState()
    data object Success : VoiceState()
    data object Error : VoiceState()
}
