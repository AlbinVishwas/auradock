package com.auradock.ai

import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.auradock.ui.template.LayoutType
import com.auradock.ui.template.StandbyTemplate
import com.auradock.ui.template.StyleConfig
import com.auradock.ui.template.WidgetConfig
import com.auradock.ui.template.WidgetPosition
import com.auradock.ui.template.WidgetType
import com.auradock.ui.template.sanitizedForStandby
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

object TemplateGenerator {
    private const val TAG = "TemplateGenerator"

    suspend fun generateTemplateFromPrompt(prompt: String): StandbyTemplate = withContext(Dispatchers.IO) {
        try {
            val rawJson = requestTemplateJson(prompt)
            val parsed = parseTemplateJson(rawJson)
            sanitizeTemplate(parsed)
        } catch (t: Throwable) {
            Log.e(TAG, "Template generation failed. Using fallback.", t)
            defaultTemplate()
        }
    }

    fun defaultTemplate(): StandbyTemplate = StandbyTemplate(
        id = "default_split",
        name = "Default Split",
        layoutType = LayoutType.SPLIT,
        widgets = listOf(
            WidgetConfig(type = WidgetType.CLOCK, position = WidgetPosition.LEFT),
            WidgetConfig(type = WidgetType.WEATHER, position = WidgetPosition.RIGHT)
        ),
        style = StyleConfig(
            accentColor = Color(0xFF4FC3F7),
            theme = "dark"
        )
    )

    private suspend fun requestTemplateJson(prompt: String): String {
        // Antigravity (Claude-style) integration placeholder.
        // Replace this with real HTTP call + auth headers + timeout handling.
        delay(250)

        val lowerPrompt = prompt.lowercase(Locale.getDefault())
        return when {
            "minimal" in lowerPrompt -> """
                {
                  "name": "Focus Mode",
                  "layoutType": "MINIMAL",
                  "widgets": [
                    { "type": "CLOCK", "position": "FULL" }
                  ],
                  "style": {
                    "theme": "dark",
                    "accentColor": "#4FC3F7"
                  }
                }
            """.trimIndent()

            "calendar" in lowerPrompt -> """
                {
                  "name": "Calendar Focus",
                  "layoutType": "SPLIT",
                  "widgets": [
                    { "type": "CLOCK", "position": "LEFT" },
                    { "type": "CALENDAR", "position": "RIGHT" }
                  ],
                  "style": {
                    "theme": "dark",
                    "accentColor": "#2979FF"
                  }
                }
            """.trimIndent()

            else -> """
                {
                  "name": "Adaptive Standby",
                  "layoutType": "SPLIT",
                  "widgets": [
                    { "type": "CLOCK", "position": "LEFT" },
                    { "type": "WEATHER", "position": "RIGHT" }
                  ],
                  "style": {
                    "theme": "dark",
                    "accentColor": "#4FC3F7"
                  }
                }
            """.trimIndent()
        }
    }

    private fun parseTemplateJson(rawJson: String): StandbyTemplate {
        val root = JsonParser.parseString(rawJson).asJsonObject

        val name = root.optionalString("name") ?: "AI Template"
        val layoutType = root.optionalEnum("layoutType", ::parseLayoutType) ?: LayoutType.SPLIT
        val widgets = parseWidgets(root.getAsJsonArray("widgets"))
        val style = parseStyle(root.getAsJsonObject("style"))

        return StandbyTemplate(
            id = "ai_${System.currentTimeMillis()}",
            name = name,
            layoutType = layoutType,
            widgets = widgets,
            style = style
        )
    }

    private fun parseWidgets(array: JsonArray?): List<WidgetConfig> {
        if (array == null) return emptyList()

        return array.mapNotNull { element ->
            val widgetObject = element as? JsonObject ?: return@mapNotNull null
            val type = widgetObject.optionalEnum("type", ::parseWidgetType) ?: return@mapNotNull null
            val position = widgetObject.optionalEnum("position", ::parseWidgetPosition) ?: return@mapNotNull null
            WidgetConfig(type = type, position = position)
        }
    }

    private fun parseStyle(styleObject: JsonObject?): StyleConfig {
        val theme = styleObject?.optionalString("theme") ?: "dark"
        val accentHex = styleObject?.optionalString("accentColor") ?: "#4FC3F7"

        val color = runCatching { Color(AndroidColor.parseColor(accentHex)) }
            .getOrElse {
                Log.e(TAG, "Invalid accentColor '$accentHex'. Using fallback color.")
                Color(0xFF4FC3F7)
            }

        return StyleConfig(
            accentColor = color,
            theme = theme
        )
    }

    private fun sanitizeTemplate(candidate: StandbyTemplate): StandbyTemplate {
        val layout = if (candidate.layoutType in LayoutType.entries) candidate.layoutType else LayoutType.SPLIT
        val validatedWidgets = candidate.widgets
            .filter { widget ->
                widget.type in WidgetType.entries && widget.position in WidgetPosition.entries
            }

        val clock = validatedWidgets.firstOrNull { it.type == WidgetType.CLOCK }
            ?: WidgetConfig(type = WidgetType.CLOCK, position = defaultClockPosition(layout))

        val secondaryWidgets = validatedWidgets
            .filterNot { it.type == WidgetType.CLOCK }
            .take(2)

        val safeTemplate = candidate.copy(
            layoutType = layout,
            widgets = listOf(clock) + secondaryWidgets
        ).sanitizedForStandby()

        if (safeTemplate.widgets.isEmpty()) {
            return defaultTemplate()
        }

        return when {
            safeTemplate.widgets.none { it.type == WidgetType.CLOCK } -> defaultTemplate()
            else -> safeTemplate
        }
    }

    private fun defaultClockPosition(layoutType: LayoutType): WidgetPosition = when (layoutType) {
        LayoutType.MINIMAL, LayoutType.FOCUS -> WidgetPosition.FULL
        LayoutType.SPLIT -> WidgetPosition.LEFT
        LayoutType.GRID -> WidgetPosition.TOP
    }

    private fun parseLayoutType(value: String): LayoutType? = runCatching {
        LayoutType.valueOf(value.uppercase(Locale.getDefault()))
    }.getOrNull()

    private fun parseWidgetType(value: String): WidgetType? = runCatching {
        WidgetType.valueOf(value.uppercase(Locale.getDefault()))
    }.getOrNull()

    private fun parseWidgetPosition(value: String): WidgetPosition? = runCatching {
        WidgetPosition.valueOf(value.uppercase(Locale.getDefault()))
    }.getOrNull()

    private fun JsonObject.optionalString(key: String): String? {
        if (!has(key) || get(key).isJsonNull) return null
        return get(key).asString
    }

    private fun <T> JsonObject.optionalEnum(key: String, parser: (String) -> T?): T? {
        val raw = optionalString(key) ?: return null
        return parser(raw)
    }
}
