package com.letschat.mvp_1;

/**
 * Pure class for push notification and timing policies.
 * No Spring dependencies, no I/O, fully testable.
 */
public final class PushPolicy {

    private PushPolicy() {}

    /**
     * Determines if a push notification should be delivered based on mute status,
     * whether the message is a reply to the user, or whether it mentions the user.
     *
     * @param muted   whether the chat is muted by the user
     * @param reply   whether the message is a reply to this user
     * @param mention whether the message mentions this user
     * @return true if notification should be sent, false otherwise
     */
    public static boolean shouldNotify(boolean muted, boolean reply, boolean mention) {
        if (!muted) {
            return true;
        }
        return reply || mention;
    }

    /**
     * Calculates the remaining delay (in milliseconds) before the debounce timer should fire,
     * honoring the debounce delay on each event while strictly enforcing a hard cap from the
     * timestamp of the first pending message.
     *
     * @param now             current timestamp in milliseconds
     * @param firstPendingAt  timestamp when the first pending message in the batch arrived
     * @param debounceDelayMs standard debounce reset window (e.g., 5000ms for chat, 30000ms for room)
     * @param maxWaitMs       hard cap on total delay (e.g., 15000ms for chat, 60000ms for room)
     * @return non-negative delay in milliseconds until the timer should fire
     */
    public static long calculateDebounceDelayMs(long now, long firstPendingAt, long debounceDelayMs, long maxWaitMs) {
        if (firstPendingAt <= 0) {
            firstPendingAt = now;
        }
        long targetReset = now + debounceDelayMs;
        long hardCap = firstPendingAt + maxWaitMs;
        long targetFire = Math.min(targetReset, hardCap);
        return Math.max(0L, targetFire - now);
    }
}
