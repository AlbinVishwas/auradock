package com.auradock.standby

enum class StandbyState {
    ACTIVE,
    INACTIVE
}

enum class StandbyTriggerMode {
    CHARGING_AND_LANDSCAPE,
    CHARGING_ONLY,
    LANDSCAPE_ONLY
}
