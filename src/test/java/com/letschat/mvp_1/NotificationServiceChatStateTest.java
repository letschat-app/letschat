package com.letschat.mvp_1;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationServiceChatStateTest {

    @Test
    void testChatStateConstants() {
        assertEquals(7L * 24 * 60 * 60 * 1000L, NotificationService.CHAT_STATE_EXPIRY_MS);
        assertEquals(24L, NotificationService.CHAT_STATE_CLEANUP_INTERVAL_HOURS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCleanupChatStatesPreservesPendingDebounceAndActiveStates() throws Exception {
        NotificationService service = new NotificationService(null, null, null, null);
        try {
            Field storeField = NotificationService.class.getDeclaredField("store");
            storeField.setAccessible(true);
            Map<String, NotificationService.ChatState> store = (Map<String, NotificationService.ChatState>) storeField.get(service);

            long now = System.currentTimeMillis();

            // 1. Idle state older than 7 days (should be removed)
            NotificationService.ChatState oldState = new NotificationService.ChatState();
            oldState.lastUpdated = now - (8L * 24 * 60 * 60 * 1000L); // 8 days old
            oldState.unread = 10;
            oldState.batchCount = 0;
            store.put("u1:c1", oldState);

            // 2. Idle state older than 7 days BUT with pending debounce (batchCount > 0) -> MUST NOT be removed
            NotificationService.ChatState oldWithBatch = new NotificationService.ChatState();
            oldWithBatch.lastUpdated = now - (8L * 24 * 60 * 60 * 1000L);
            oldWithBatch.unread = 5;
            oldWithBatch.batchCount = 2; // pending debounce!
            store.put("u2:c2", oldWithBatch);

            // 3. Recent state 2 days old (under 7 days) -> MUST NOT be removed
            NotificationService.ChatState recentState = new NotificationService.ChatState();
            recentState.lastUpdated = now - (2L * 24 * 60 * 60 * 1000L);
            recentState.unread = 1;
            recentState.batchCount = 0;
            store.put("u3:c3", recentState);

            // Run cleanupChatStates
            service.cleanupChatStates();

            // Assertions
            assertFalse(store.containsKey("u1:c1"), "Idle state older than 7 days should be removed");
            assertTrue(store.containsKey("u2:c2"), "State with pending batchCount > 0 must not be removed");
            assertTrue(store.containsKey("u3:c3"), "Recent state (2 days old) must not be removed");
        } finally {
            service.shutdown();
        }
    }
}
