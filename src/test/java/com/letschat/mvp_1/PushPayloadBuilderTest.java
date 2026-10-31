package com.letschat.mvp_1;

import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PushPayloadBuilderTest {

    @Test
    void testSanitizePreview() {
        String input = "Hello   <@u123:Priya>  how   are  \n you doing?  ";
        String sanitized = PushPayloadBuilder.sanitizePreview(input);
        assertEquals("Hello @Priya how are you doing?", sanitized);

        String simpleMention = "Ping <@u456> check this";
        assertEquals("Ping @u456 check this", PushPayloadBuilder.sanitizePreview(simpleMention));

        String longStr = "A".repeat(200);
        assertEquals(120, PushPayloadBuilder.sanitizePreview(longStr).length());
    }

    @Test
    void testBuildChatPayloadAndMessage() {
        List<Map<String, String>> items = new ArrayList<>();
        Map<String, String> item1 = new LinkedHashMap<>();
        item1.put("s", "Priya");
        item1.put("t", "text");
        item1.put("x", "Are we deploying today?");
        items.add(item1);

        Map<String, String> data = PushPayloadBuilder.buildChatData(
                "chat_100",
                "Priya",
                "Priya",
                "dm",
                "message",
                "u_priya",
                "Priya",
                "text",
                "msg_001",
                "0",
                "Are we deploying today?",
                items,
                1,
                1,
                1730103300000L,
                false,
                false,
                Collections.singletonList("0/%20/text/%20/Priya/%20/Are we deploying today?"),
                "dm",
                ""
        );

        assertEquals("2", data.get("v"));
        assertEquals("CHAT", data.get("type"));
        assertEquals("chat_100", data.get("chatId"));
        assertEquals("Priya", data.get("chatName"));
        assertEquals("dm", data.get("chatKind"));
        assertEquals("1", data.get("newCount"));
        assertEquals("1", data.get("unread"));

        // All values must be non-null Strings
        for (Map.Entry<String, String> e : data.entrySet()) {
            assertNotNull(e.getKey());
            assertNotNull(e.getValue());
        }

        // Test Message creation
        Message msg = PushPayloadBuilder.buildChatMessage(data, "fake-fcm-token-123");
        assertNotNull(msg);
    }

    @Test
    void testBuildEventReminderPayloadAndMessage() {
        Map<String, String> data = PushPayloadBuilder.buildEventReminderData(
                "101",
                "Team lunch",
                "chat_200",
                "Team Lunch",
                "2026-10-28T07:00:00Z",
                15,
                "Cafeteria",
                "offline",
                "",
                "Team lunch"
        );

        assertEquals("2", data.get("v"));
        assertEquals("EVENT", data.get("type"));
        assertEquals("reminder", data.get("action"));
        assertEquals("15", data.get("minutesBefore"));
        assertEquals("Team lunch", data.get("eventTitle"));

        Message msg = PushPayloadBuilder.buildEventReminderMessage(data, "fake-fcm-token-123");
        assertNotNull(msg);
    }

    @Test
    void testBuildEventChangedPayloadAndMessage() {
        List<Map<String, String>> changes = new ArrayList<>();
        Map<String, String> diff = new LinkedHashMap<>();
        diff.put("field", "time");
        diff.put("from", "2026-10-28T07:00:00Z");
        diff.put("to", "2026-10-28T07:30:00Z");
        changes.add(diff);

        Map<String, String> data = PushPayloadBuilder.buildEventChangedData(
                "rescheduled",
                "101",
                "Team lunch",
                "chat_200",
                "Team Lunch",
                "2026-10-28T07:30:00Z",
                changes,
                "",
                "Cafeteria",
                "Team lunch",
                "Rescheduled to 2026-10-28T07:30:00Z"
        );

        assertEquals("2", data.get("v"));
        assertEquals("EVENT_CHANGED", data.get("type"));
        assertEquals("rescheduled", data.get("action"));
        assertTrue(data.get("changes").contains("field"));

        Message msg = PushPayloadBuilder.buildEventChangedMessage(data, "fake-fcm-token-123");
        assertNotNull(msg);
    }

    @Test
    void testFitToLimitTrimsOversizedPayload() {
        Map<String, String> largeData = new LinkedHashMap<>();
        largeData.put("v", "2");
        largeData.put("type", "CHAT");
        largeData.put("chatId", "chat_large");

        List<Map<String, String>> largeItems = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Map<String, String> item = new HashMap<>();
            item.put("s", "User" + i);
            item.put("t", "text");
            item.put("x", "Very long text content that takes up space in the push payload ".repeat(5));
            largeItems.add(item);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            largeData.put("items", mapper.writeValueAsString(largeItems));
        } catch (Exception ignored) {}

        assertTrue(PushPayloadBuilder.calculatePayloadSize(largeData) > 3500);

        Map<String, String> fitted = PushPayloadBuilder.fitToLimit(largeData);
        assertTrue(PushPayloadBuilder.calculatePayloadSize(fitted) <= 3500);
    }
}
