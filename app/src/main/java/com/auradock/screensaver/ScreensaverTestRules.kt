package com.auradock.screensaver

/** If the screensaver has not started by itself this long after install, the dashboard offers a gentle hint. */
const val SCREENSAVER_HINT_AFTER_MS = 24L * 60 * 60 * 1000

/** A screen timeout up to five minutes is short enough for the test: the screensaver starts soon after it passes. */
const val SCREEN_TIMEOUT_OK_MAX_MS = 5L * 60 * 1000

fun timeoutOk(screenOffTimeoutMs: Long): Boolean = screenOffTimeoutMs in 1..SCREEN_TIMEOUT_OK_MAX_MS

/** What the dashboard says about the screensaver test, if anything. */
enum class ScreensaverBanner {
    NONE,

    /** "AuraDock appeared on its own": the dream started since the app was last open. Shown until dismissed. */
    APPEARED,

    /** A day after install and still no screensaver: a quiet line with "Try again". */
    HINT
}

/**
 * Which note the dashboard shows. [verifiedAt] is when the app was first opened after the dream appeared (0: not yet);
 * [dreamAt] is when the dream first started with the gate passing (0: never); [hintSnoozedUntil] is set when "Try again" was used.
 */
fun screensaverBanner(
    dreamAt: Long,
    verifiedAt: Long,
    bannerDismissed: Boolean,
    installedAt: Long,
    now: Long,
    hintSnoozedUntil: Long
): ScreensaverBanner = when {
    verifiedAt > 0 -> if (bannerDismissed) ScreensaverBanner.NONE else ScreensaverBanner.APPEARED
    dreamAt > 0 -> ScreensaverBanner.NONE // it happened; the note comes on the next app open
    now - installedAt >= SCREENSAVER_HINT_AFTER_MS && now >= hintSnoozedUntil -> ScreensaverBanner.HINT
    else -> ScreensaverBanner.NONE
}

/** The first test step still to do, in order: choose it, set the timeout, then wait on the stand. */
enum class ScreensaverNext { CHOOSE, TIMEOUT, WAIT, DONE }

fun nextScreensaverStep(selected: Boolean, timeoutOk: Boolean, appeared: Boolean): ScreensaverNext = when {
    !selected -> ScreensaverNext.CHOOSE
    !timeoutOk -> ScreensaverNext.TIMEOUT
    !appeared -> ScreensaverNext.WAIT
    else -> ScreensaverNext.DONE
}
