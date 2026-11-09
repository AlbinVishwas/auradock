package com.auradock.dock.split

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

/**
 * The month as rows of seven cells (null: an empty cell), starting each row on [firstDay] (the phone's first day of the week; the
 * frames show Monday). A cell holds the day of the month.
 */
fun monthCells(month: YearMonth, firstDay: DayOfWeek): List<Int?> {
    val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val cells = ArrayList<Int?>()
    repeat(lead) { cells += null }
    for (d in 1..month.lengthOfMonth()) cells += d
    while (cells.size % 7 != 0) cells += null
    return cells
}

/** The seven weekday initials, starting with [firstDay]: "M T W T F S S" for Monday, "S M T W T F S" for Sunday. */
fun weekdayInitials(firstDay: DayOfWeek, locale: Locale): List<String> =
    (0 until 7).map { firstDay.plus(it.toLong()).getDisplayName(TextStyle.NARROW, locale).uppercase(locale) }

/** The phone's first day of the week (it follows the region, and Android 14's own setting): [Calendar] knows both. */
fun phoneFirstDay(locale: Locale = Locale.getDefault()): DayOfWeek {
    val c = Calendar.getInstance(locale).firstDayOfWeek // Calendar.SUNDAY = 1 ... SATURDAY = 7
    return if (c == Calendar.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek.of(c - 1)
}

/** "NOVEMBER", in the phone's language. */
fun monthTitle(date: LocalDate, locale: Locale): String =
    date.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).uppercase(locale)
