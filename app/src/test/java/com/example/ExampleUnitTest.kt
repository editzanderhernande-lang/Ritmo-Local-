package com.example

import com.example.model.TimeOfDaySlot
import com.example.model.formatDurationMs
import com.example.model.formatPlaybackTimeReadable
import com.example.model.stableTrackStatId
import com.example.player.SleepTimerEngine
import com.example.player.formatSleepTimerCountdown
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun timeOfDaySlot_mapsHoursAccurately() {
        assertEquals(TimeOfDaySlot.MORNING, TimeOfDaySlot.fromHour(5))
        assertEquals(TimeOfDaySlot.MORNING, TimeOfDaySlot.fromHour(11))
        assertEquals(TimeOfDaySlot.AFTERNOON, TimeOfDaySlot.fromHour(12))
        assertEquals(TimeOfDaySlot.AFTERNOON, TimeOfDaySlot.fromHour(18))
        assertEquals(TimeOfDaySlot.NIGHT, TimeOfDaySlot.fromHour(19))
        assertEquals(TimeOfDaySlot.NIGHT, TimeOfDaySlot.fromHour(2))
    }

    @Test
    fun durationAndPlaybackFormatters_formatCorrectly() {
        assertEquals("03:45", formatDurationMs(225_000L))
        assertEquals("1:01:05", formatDurationMs(3_665_000L))
        assertEquals("45s", formatPlaybackTimeReadable(45L))
        assertEquals("12m 5s", formatPlaybackTimeReadable(725L))
        assertEquals("2h 1m 3s", formatPlaybackTimeReadable(7_263L))
        assertEquals("05:00", formatSleepTimerCountdown(300L))
        assertEquals("1:05:09", formatSleepTimerCountdown(3_909L))
    }

    @Test
    fun stableTrackStatId_producesValidFirestoreDocumentId() {
        val id = stableTrackStatId("mi canción|artista ñ|210")
        assertTrue(id.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
        assertTrue(id.length <= 128)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun sleepTimerEngine_countsDownAndTriggersCallbackOnExpiry() = runTest {
        var expiredCount = 0
        val engine = SleepTimerEngine(
            scope = backgroundScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            nowWallClockMs = { 1_000L + testScheduler.currentTime },
            tickIntervalMs = 250L,
            onExpired = { expiredCount++ }
        )

        engine.startCountdown(5L)
        runCurrent()
        assertTrue(engine.state.value.isActive)
        assertEquals(5L, engine.state.value.remainingSeconds)

        advanceTimeBy(3_000L)
        runCurrent()
        assertTrue(engine.state.value.isActive)
        assertEquals(2L, engine.state.value.remainingSeconds)
        assertEquals(0, expiredCount)

        advanceTimeBy(2_250L)
        runCurrent()
        assertFalse(engine.state.value.isActive)
        assertEquals(0L, engine.state.value.remainingSeconds)
        assertEquals(1, expiredCount)
        assertNotNull(engine.state.value.justExpiredAtMs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun sleepTimerEngine_extendAndCancelWorkCorrectly() = runTest {
        var expiredCount = 0
        val engine = SleepTimerEngine(
            scope = backgroundScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            tickIntervalMs = 250L,
            onExpired = { expiredCount++ }
        )

        engine.startCountdown(10L)
        advanceTimeBy(4_000L)
        runCurrent()
        engine.addSeconds(30L)
        assertTrue(engine.state.value.isActive)
        assertEquals(36L, engine.state.value.remainingSeconds)

        engine.cancel()
        assertFalse(engine.state.value.isActive)
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(0, expiredCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun sleepTimerEngine_stopAfterCurrentTrackConsumesOnce() = runTest {
        val engine = SleepTimerEngine(
            scope = backgroundScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            onExpired = {}
        )

        engine.setStopAfterCurrentTrack(true)
        assertTrue(engine.state.value.isActive)
        assertTrue(engine.state.value.stopAfterCurrentTrack)

        assertTrue(engine.consumeStopAfterCurrentTrack())
        assertFalse(engine.state.value.isActive)
        assertFalse(engine.state.value.stopAfterCurrentTrack)
        assertFalse(engine.consumeStopAfterCurrentTrack())
    }
}

