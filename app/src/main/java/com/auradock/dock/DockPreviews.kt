package com.auradock.dock

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.auradock.ui.preview.PreviewData
import com.auradock.ui.preview.PreviewLandscape
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.theme.AuraDockTheme

// The dock is landscape-only, so every template has landscape previews. A portrait one is added too, to see how a template
// holds up if the window is ever the wrong way round (the dock normally rotates its content, see RotatedContent).

@PreviewLandscape @Composable private fun BoldFull() = AuraDockTheme { DockContent(DockTemplate.BOLD, PreviewData.dockFull) }
@PreviewLandscape @Composable private fun BoldEmpty() = AuraDockTheme { DockContent(DockTemplate.BOLD, PreviewData.dockEmpty) }
@PreviewPortrait @Composable private fun BoldPortrait() = AuraDockTheme { DockContent(DockTemplate.BOLD, PreviewData.dockFull) }

@PreviewLandscape @Composable private fun ClassicFull() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, PreviewData.dockFull) }
@PreviewLandscape @Composable private fun ClassicEmpty() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, PreviewData.dockEmpty) }

@PreviewLandscape @Composable private fun FocusFull() = AuraDockTheme { DockContent(DockTemplate.FOCUS, PreviewData.dockFull) }
@PreviewLandscape @Composable private fun FocusEmpty() = AuraDockTheme { DockContent(DockTemplate.FOCUS, PreviewData.dockEmpty) }

@PreviewLandscape @Composable private fun MinimalFull() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, PreviewData.dockFull) }
@PreviewLandscape @Composable private fun MinimalEmpty() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, PreviewData.dockEmpty) }

@PreviewLandscape @Composable private fun ProductivityFull() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, PreviewData.dockFull) }

/** Many tasks and events, to see "+N more". */
@PreviewLandscape
@Composable
private fun ProductivityBusy() = AuraDockTheme {
    DockContent(
        DockTemplate.PRODUCTIVITY,
        PreviewData.dockFull.copy(tasks = PreviewData.tasks + PreviewData.tasks.map { it.copy(id = it.id + 10, done = false) })
    )
}

@PreviewLandscape @Composable private fun ProductivityEmpty() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, PreviewData.dockEmpty) }

@PreviewPortrait @Composable private fun ClassicPortrait() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, PreviewData.dockFull) }
@PreviewPortrait @Composable private fun ProductivityPortrait() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, PreviewData.dockFull) }

// The focus session line: a session 25 minutes in, on the templates that carry it, and a fresh one (nothing filled yet).
private val inSession = PreviewData.dockFull.copy(session = SessionSnapshot(startedAt = PreviewData.dockFull.timeMillis - 25 * 60_000L))
@PreviewLandscape @Composable private fun BoldSession() = AuraDockTheme { DockContent(DockTemplate.BOLD, inSession) }
@PreviewLandscape @Composable private fun ClassicSession() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, inSession) }
@PreviewLandscape @Composable private fun MinimalSession() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, inSession) }
@PreviewLandscape @Composable private fun BoldSessionJustStarted() = AuraDockTheme {
    DockContent(DockTemplate.BOLD, PreviewData.dockFull.copy(session = SessionSnapshot(startedAt = PreviewData.dockFull.timeMillis)))
}

// "Held for you": counts with and without a notification line, and the quiet session that shows nothing.
private val heldSession = SessionSnapshot(startedAt = PreviewData.dockFull.timeMillis - 40 * 60_000L, held = 6, through = 2)
@PreviewLandscape @Composable private fun BoldHeld() = AuraDockTheme { DockContent(DockTemplate.BOLD, PreviewData.dockFull.copy(session = heldSession)) }
@PreviewLandscape @Composable private fun ClassicHeld() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, PreviewData.dockFull.copy(session = heldSession)) }
@PreviewLandscape @Composable private fun MinimalHeld() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, PreviewData.dockFull.copy(session = heldSession)) }
@PreviewLandscape @Composable private fun BoldQuietSession() = AuraDockTheme {
    DockContent(DockTemplate.BOLD, PreviewData.dockFull.copy(session = heldSession.copy(held = 0, through = 0)))
}

// The quiet lines under the clock: the morning intention open and done, and the morning question.
private val withIntention = PreviewData.dockFull.copy(lines = com.auradock.dock.lines.DockLines(intention = com.auradock.intention.IntentionLine("Ship the invoice to Meera", done = false)))
private val withIntentionDone = PreviewData.dockFull.copy(lines = com.auradock.dock.lines.DockLines(intention = com.auradock.intention.IntentionLine("Ship the invoice to Meera", done = true)))
@PreviewLandscape @Composable private fun BoldIntention() = AuraDockTheme { DockContent(DockTemplate.BOLD, withIntention) }
@PreviewLandscape @Composable private fun BoldIntentionDone() = AuraDockTheme { DockContent(DockTemplate.BOLD, withIntentionDone) }
@PreviewLandscape @Composable private fun ClassicIntention() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, withIntention) }
@PreviewLandscape @Composable private fun FocusIntention() = AuraDockTheme { DockContent(DockTemplate.FOCUS, withIntention) }
@PreviewLandscape @Composable private fun ProductivityIntention() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, withIntention) }
@PreviewLandscape @Composable private fun MinimalIntention() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, withIntention) }
@PreviewLandscape @Composable private fun IntentionQuestion() = AuraDockTheme { DockContent(DockTemplate.BOLD, PreviewData.dockFull.copy(intentionPrompt = "")) }
@PreviewLandscape @Composable private fun IntentionQuestionSuggested() = AuraDockTheme { DockContent(DockTemplate.BOLD, PreviewData.dockFull.copy(intentionPrompt = "Draft the Q1 plan")) }

// The next-meeting line: counting down at 8 minutes, pulsing at 2, and stacked over the intention.
private fun withMeeting(minutes: Int, intention: Boolean = true) = PreviewData.dockFull.copy(
    lines = com.auradock.dock.lines.DockLines(
        meeting = com.auradock.data.MeetingCountdown("Design review with Meera", minutes),
        intention = if (intention) com.auradock.intention.IntentionLine("Ship the invoice to Meera", done = false) else null
    )
)
@PreviewLandscape @Composable private fun BoldMeeting() = AuraDockTheme { DockContent(DockTemplate.BOLD, withMeeting(8)) }
@PreviewLandscape @Composable private fun BoldMeetingPulsing() = AuraDockTheme { DockContent(DockTemplate.BOLD, withMeeting(2)) }
@PreviewLandscape @Composable private fun ClassicMeeting() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, withMeeting(8)) }
@PreviewLandscape @Composable private fun FocusMeeting() = AuraDockTheme { DockContent(DockTemplate.FOCUS, withMeeting(8)) }
@PreviewLandscape @Composable private fun ProductivityMeeting() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, withMeeting(8)) }
@PreviewLandscape @Composable private fun MinimalMeeting() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, withMeeting(8, intention = false)) }

// "Pick up: <task>" as the first line, with a meeting and the intention below it (the busiest the stack gets).
@PreviewLandscape @Composable private fun BoldPickUp() = AuraDockTheme {
    DockContent(DockTemplate.BOLD, PreviewData.dockFull.copy(lines = com.auradock.dock.lines.DockLines(resume = "Ship the invoice to Meera")))
}
@PreviewLandscape @Composable private fun ClassicPickUpStacked() = AuraDockTheme {
    DockContent(
        DockTemplate.CLASSIC,
        PreviewData.dockFull.copy(
            lines = com.auradock.dock.lines.DockLines(
                resume = "Ship the invoice to Meera",
                meeting = com.auradock.data.MeetingCountdown("Design review", 6),
                intention = com.auradock.intention.IntentionLine("Ship the invoice to Meera", false)
            )
        )
    )
}

/** The dock after a touch, with Focus on: the "Undo focus" control shows for four seconds. */
@PreviewLandscape
@androidx.compose.runtime.Composable
private fun DockUndoFocus() = com.auradock.ui.theme.AuraDockTheme {
    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
        DockContent(template = DockTemplate.BOLD, state = PreviewData.dockFull)
        UndoFocusPill(visible = true, onUndo = {}, modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.TopCenter).padding(top = com.auradock.ui.theme.AuraDockSpacing.xl))
    }
}
