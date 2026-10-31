package com.letschat.mvp_1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PushPolicyTest {

    @Test
    void testShouldNotifyWhenUnmuted() {
        assertTrue(PushPolicy.shouldNotify(false, false, false));
        assertTrue(PushPolicy.shouldNotify(false, true, false));
        assertTrue(PushPolicy.shouldNotify(false, false, true));
        assertTrue(PushPolicy.shouldNotify(false, true, true));
    }

    @Test
    void testShouldNotifyWhenMuted() {
        assertFalse(PushPolicy.shouldNotify(true, false, false));
        assertTrue(PushPolicy.shouldNotify(true, true, false));
        assertTrue(PushPolicy.shouldNotify(true, false, true));
        assertTrue(PushPolicy.shouldNotify(true, true, true));
    }

    @Test
    void testCalculateDebounceDelayMsChat() {
        long firstPendingAt = 100000L;
        long debounceDelayMs = 5000L;
        long maxWaitMs = 15000L;

        // Message at t=0: delay is standard 5000ms
        assertEquals(5000L, PushPolicy.calculateDebounceDelayMs(firstPendingAt, firstPendingAt, debounceDelayMs, maxWaitMs));

        // Message at t=4s: reset target is 4s+5s=9s, cap is 15s -> returns 5000ms
        assertEquals(5000L, PushPolicy.calculateDebounceDelayMs(firstPendingAt + 4000L, firstPendingAt, debounceDelayMs, maxWaitMs));

        // Message at t=9s: reset target is 9s+5s=14s, cap is 15s -> returns 5000ms
        assertEquals(5000L, PushPolicy.calculateDebounceDelayMs(firstPendingAt + 9000L, firstPendingAt, debounceDelayMs, maxWaitMs));

        // Message at t=12s: reset target is 12s+5s=17s, BUT cap is 15s! -> returns 15s-12s = 3000ms
        assertEquals(3000L, PushPolicy.calculateDebounceDelayMs(firstPendingAt + 12000L, firstPendingAt, debounceDelayMs, maxWaitMs));

        // Message past cap at t=16s: returns 0ms (timer fires immediately)
        assertEquals(0L, PushPolicy.calculateDebounceDelayMs(firstPendingAt + 16000L, firstPendingAt, debounceDelayMs, maxWaitMs));
    }

    @Test
    void testCalculateDebounceDelayMsRoom() {
        long firstPendingAt = 200000L;
        long debounceDelayMs = 30000L;
        long maxWaitMs = 60000L;

        // Initial t=0: delay is 30s
        assertEquals(30000L, PushPolicy.calculateDebounceDelayMs(firstPendingAt, firstPendingAt, debounceDelayMs, maxWaitMs));

        // Reset at t=40s: 40s+30s=70s, capped at 60s -> remaining delay is 20s
        assertEquals(20000L, PushPolicy.calculateDebounceDelayMs(firstPendingAt + 40000L, firstPendingAt, debounceDelayMs, maxWaitMs));
    }
}
