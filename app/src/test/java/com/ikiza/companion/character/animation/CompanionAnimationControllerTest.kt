package com.ikiza.companion.character.animation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionAnimationControllerTest {

    @Test
    fun testInitialStateIsIdle() = runTest {
        val controller = CompanionAnimationController(this, inactivityTimeoutMs = 10_000L)
        assertEquals(CompanionAnimationState.Idle, controller.state.value)
    }

    @Test
    fun testGreetingReactionTransitionsToIdleAfterDuration() = runTest {
        val controller = CompanionAnimationController(this, inactivityTimeoutMs = 10_000L)
        controller.onGreeting(durationMs = 1_500L)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Greeting, controller.state.value)

        testScheduler.advanceTimeBy(1_600L)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Idle, controller.state.value)
    }

    @Test
    fun testNotificationReactionTransitionsToIdle() = runTest {
        val controller = CompanionAnimationController(this, inactivityTimeoutMs = 10_000L)
        controller.onNotificationReceived(durationMs = 1_200L)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.NotificationReaction, controller.state.value)

        testScheduler.advanceTimeBy(1_300L)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Idle, controller.state.value)
    }

    @Test
    fun testSpeakingStateSynchronizedWithSpeech() = runTest {
        val controller = CompanionAnimationController(this, inactivityTimeoutMs = 10_000L)
        controller.onSpeechStateChanged(true)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Speaking, controller.state.value)

        controller.onSpeechStateChanged(false)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Idle, controller.state.value)
    }

    @Test
    fun testCallReactionTakesPriorityOverOtherReactions() = runTest {
        val controller = CompanionAnimationController(this, inactivityTimeoutMs = 10_000L)
        controller.onCallStateChanged(true)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.CallReaction, controller.state.value)

        // Notification received during incoming call should not override call alert
        controller.onNotificationReceived(durationMs = 1_000L)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.CallReaction, controller.state.value)

        controller.onCallStateChanged(false)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Idle, controller.state.value)
    }

    @Test
    fun testThinkingStateDuringTask() = runTest {
        val controller = CompanionAnimationController(this, inactivityTimeoutMs = 10_000L)
        controller.onTaskStateChanged(true)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Thinking, controller.state.value)

        controller.onTaskStateChanged(false)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Idle, controller.state.value)
    }

    @Test
    fun testInactivityTransitionsToSleepyAndUserInteractionWakesUp() = runTest {
        val controller = CompanionAnimationController(this, inactivityTimeoutMs = 5_000L)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Idle, controller.state.value)

        testScheduler.advanceTimeBy(5_100L)
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Sleepy, controller.state.value)

        controller.onUserInteraction()
        testScheduler.runCurrent()
        assertEquals(CompanionAnimationState.Idle, controller.state.value)
    }
}
