package com.github.damontecres.wholphin.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Recover the calendar date of a Jellyfin premiere date.
 *
 * The SDK deserializes UTC date-only metadata into a device-local [LocalDateTime].
 * Convert that value back to UTC before taking its date, so western timezones do
 * not display the previous day. Actual timestamps should retain their local date.
 */
fun LocalDateTime.toPremiereDate(): LocalDate =
    atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDate()
