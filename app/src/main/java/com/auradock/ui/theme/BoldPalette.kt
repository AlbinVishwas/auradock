package com.auradock.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colourways of the Bold clock. Each alternates two or three tones of one hue across the digits (first digit tone 1, second
 * tone 2, ...). The night/dim state is applied to the finished clock, so it dims every colourway the same way.
 */
enum class BoldPalette(val label: String, val tones: List<Color>) {
    Coral("Coral", listOf(Color(0xFFFF5A4E), Color(0xFFFF8F86))),
    Blue("Blue", listOf(Color(0xFF2F6BFF), Color(0xFF5C8DFF), Color(0xFF8FB0FF))),
    Pink("Pink", listOf(Color(0xFFFF3D8B), Color(0xFFFF7AB0))),
    Violet("Violet", listOf(Color(0xFFB04BD8), Color(0xFF9A7BFF), Color(0xFFD07BC4))),
    Amber("Amber", listOf(Color(0xFFFF9F1C), Color(0xFFFFC266))),
    Mint("Mint", listOf(Color(0xFF2FD39A), Color(0xFF7BE8C1))),
    Mono("Mono", listOf(Color(0xFFEDEDED), Color(0xFFBDBDBD)));

    /** The tone for the [index]th digit. */
    fun tone(index: Int): Color = tones[index % tones.size]

    companion object {
        val default = Coral
        fun parse(name: String?): BoldPalette = entries.firstOrNull { it.name == name } ?: default
    }
}
