package com.github.damontecres.wholphin.ui

import com.github.damontecres.wholphin.data.model.BaseItem
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.serializer.DateTimeSerializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@RunWith(Parameterized::class)
class PremiereDateTest(
    private val timezone: String,
    private val airDate: String,
) {
    private lateinit var originalTimezone: TimeZone
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalTimezone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(timezone))
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimezone)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun preservesCalendarDateAfterSdkDeserialization() {
        val decoded = Json.decodeFromString(DateTimeSerializer(), "\"" + airDate + "T00:00:00.0000000Z\"")
        val expected = LocalDate.parse(airDate)

        assertEquals(expected, decoded.toPremiereDate())
        assertEquals(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.US).format(expected),
            formatPremiereDate(decoded),
        )
        // Ordinary timestamps still display the SDK's local calendar date.
        assertEquals(getDateFormatter().format(decoded), formatDateTime(decoded))
    }

    @Test
    fun episodeMetadataAndSubtitleUseTheSameAirDate() {
        val decoded = Json.decodeFromString(DateTimeSerializer(), "\"" + airDate + "T00:00:00.0000000Z\"")
        val item =
            BaseItem(
                BaseItemDto(
                    id = UUID.randomUUID(),
                    type = BaseItemKind.EPISODE,
                    name = airDate,
                    premiereDate = decoded,
                    parentIndexNumber = 2026,
                    indexNumber = 316,
                ),
            )
        val expected =
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.US)
                .format(LocalDate.parse(airDate))

        assertTrue(item.subtitleLong!!.endsWith(expected))
        assertTrue(
            item.ui.quickDetails.basic.text
                .contains(expected),
        )
    }

    @Test
    fun dateOnlyCardAndProgramMetadataUseAirDate() {
        val decoded = Json.decodeFromString(DateTimeSerializer(), "\"" + airDate + "T00:00:00.0000000Z\"")
        val data =
            BaseItemDto(
                id = UUID.randomUUID(),
                type = BaseItemKind.EPISODE,
                name = airDate,
                premiereDate = decoded,
            )
        val expected =
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.US)
                .format(LocalDate.parse(airDate))

        assertEquals(expected, BaseItem(data).ui.episodeCornerText)
        val program = BaseItem(data.copy(type = BaseItemKind.PROGRAM, isSeries = true))
        assertTrue(
            program.ui.quickDetails.basic.text
                .contains(expected),
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {1}")
        fun parameters(): List<Array<String>> =
            listOf(
                "America/Los_Angeles",
                "America/New_York",
                "UTC",
                "Europe/Berlin",
                "Asia/Tokyo",
                "Pacific/Kiritimati",
            ).flatMap { zone ->
                listOf(
                    "2026-11-12",
                    "2026-07-12",
                    "2026-01-01",
                    "2026-03-08",
                    "2026-11-01",
                    "2024-02-29",
                ).map { date -> arrayOf(zone, date) }
            }
    }
}
