package com.auradock.focus

const val MAX_PICKED_APPS = 3

/**
 * Picks or un-picks an app: tapping a picked app removes it; tapping an unpicked one adds it, unless [max] are already
 * picked, in which case the list is returned unchanged.
 */
fun togglePick(current: List<String>, packageName: String, max: Int = MAX_PICKED_APPS): List<String> = when {
    packageName in current -> current - packageName
    current.size >= max -> current
    else -> current + packageName
}

/** Whether tapping this app would do something (un-pick always can; pick only while there is room). */
fun canToggle(current: List<String>, packageName: String, max: Int = MAX_PICKED_APPS): Boolean =
    packageName in current || current.size < max
