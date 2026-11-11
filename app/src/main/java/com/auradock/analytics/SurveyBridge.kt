package com.auradock.analytics

import com.posthog.PostHog
import com.posthog.surveys.PostHogDisplayOpenQuestion
import com.posthog.surveys.PostHogDisplaySurvey
import com.posthog.surveys.PostHogNextSurveyQuestion
import com.posthog.surveys.PostHogSurveyResponse
import com.posthog.surveys.PostHogSurveysDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A survey waiting to be shown: its open-text question, and the callbacks that report back to PostHog. */
class PendingSurvey(
    val title: String,
    val question: String,
    val buttonText: String,
    private val survey: PostHogDisplaySurvey,
    private val shown: (PostHogDisplaySurvey) -> Unit,
    private val respond: (PostHogDisplaySurvey, Int, PostHogSurveyResponse) -> PostHogNextSurveyQuestion?,
    private val closed: (PostHogDisplaySurvey) -> Unit
) {
    fun onShown() = shown(survey)
    fun send(text: String) { respond(survey, 0, PostHogSurveyResponse.Text(text)) }
    fun dismiss() = closed(survey)
}

/**
 * Receives surveys from PostHog and holds the latest one until the dashboard shows it (see `SurveyHost`). Nothing is drawn here, so a survey
 * can never appear on the dock: the dock has no host. Only surveys whose first question is free text are shown ("What's missing?"); any
 * other kind is closed without being shown. A survey also waits until the app has been in use for [MIN_DAYS_OF_USE] days.
 */
object SurveyBridge : PostHogSurveysDelegate {
    const val MIN_DAYS_OF_USE = 7L

    private val _pending = MutableStateFlow<PendingSurvey?>(null)
    val pending: StateFlow<PendingSurvey?> = _pending.asStateFlow()

    override fun renderSurvey(
        survey: PostHogDisplaySurvey,
        onSurveyShown: (PostHogDisplaySurvey) -> Unit,
        onSurveyResponse: (PostHogDisplaySurvey, Int, PostHogSurveyResponse) -> PostHogNextSurveyQuestion?,
        onSurveyClosed: (PostHogDisplaySurvey) -> Unit
    ) {
        val first = survey.questions.firstOrNull() as? PostHogDisplayOpenQuestion
        if (first == null || survey.questions.size != 1) {
            onSurveyClosed(survey)
            return
        }
        _pending.value = PendingSurvey(survey.name, first.question, first.buttonText ?: "Send", survey, onSurveyShown, onSurveyResponse, onSurveyClosed)
    }

    override fun cleanupSurveys() {
        _pending.value = null
    }

    /** True once the app has been used for long enough to be asked. */
    fun eligible(firstUseAt: Long, now: Long): Boolean = now - firstUseAt >= MIN_DAYS_OF_USE * 86_400_000L

    fun clear() { _pending.value = null }
}
