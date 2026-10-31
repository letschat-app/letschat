package com.letschat.mvp_1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.WebpushConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure class for constructing FCM v2 data payloads and Message instances.
 * Guarantees data-only pushes (no FCM notification block), non-null String values,
 * and payloads bounded to <= 3500 UTF-8 bytes.
 */
public final class PushPayloadBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PushPayloadBuilder.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_PAYLOAD_BYTES = 3500;
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@([^:>]+):([^>]+)>");
    private static final Pattern SIMPLE_MENTION_PATTERN = Pattern.compile("<@([^>]+)>");

    private PushPayloadBuilder() {}

    /**
     * Sanitizes message preview:
     * - Rewrites <@id:Name> to @Name and <@id> to @id
     * - Collapses multiple whitespace / newlines into a single space and trims
     * - Caps text length to max 120 characters
     */
    public static String sanitizePreview(String text) {
        if (text == null) {
            return "";
        }
        String result = MENTION_PATTERN.matcher(text).replaceAll("@$2");
        result = SIMPLE_MENTION_PATTERN.matcher(result).replaceAll("@$1");
        result = result.replaceAll("\\s+", " ").trim();
        if (result.length() > 120) {
            return result.substring(0, 120);
        }
        return result;
    }

    /**
     * Calculates the total UTF-8 byte size of all keys and values in the payload map.
     */
    public static int calculatePayloadSize(Map<String, String> data) {
        if (data == null) return 0;
        int total = 0;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey() != null) {
                total += entry.getKey().getBytes(StandardCharsets.UTF_8).length;
            }
            if (entry.getValue() != null) {
                total += entry.getValue().getBytes(StandardCharsets.UTF_8).length;
            }
        }
        return total;
    }

    /**
     * Enforces the <= 3500 bytes UTF-8 FCM limit on data payloads.
     * Trims items/msgs from oldest first, truncates preview if needed.
     */
    public static Map<String, String> fitToLimit(Map<String, String> originalData) {
        if (originalData == null) {
            return new LinkedHashMap<>();
        }
        Map<String, String> data = new LinkedHashMap<>(originalData);
        int currentSize = calculatePayloadSize(data);
        if (currentSize <= MAX_PAYLOAD_BYTES) {
            return data;
        }

        logger.warn("[PUSH] Payload size {} exceeds {} bytes limit, trimming...", currentSize, MAX_PAYLOAD_BYTES);

        // Attempt 1: Trim items and msgs lists if present
        String itemsJson = data.get("items");
        String msgsJson = data.get("msgs");

        if (itemsJson != null) {
            try {
                List<Map<String, String>> items = objectMapper.readValue(itemsJson, new TypeReference<List<Map<String, String>>>() {});
                List<String> msgs = msgsJson != null
                        ? objectMapper.readValue(msgsJson, new TypeReference<List<String>>() {})
                        : new ArrayList<>();

                // Truncate each item preview in items list to 100 chars
                for (Map<String, String> it : items) {
                    String x = it.get("x");
                    if (x != null && x.length() > 100) {
                        it.put("x", x.substring(0, 100));
                    }
                }
                data.put("items", objectMapper.writeValueAsString(items));
                if (calculatePayloadSize(data) <= MAX_PAYLOAD_BYTES) {
                    return data;
                }

                // Drop oldest items one by one
                while (items.size() > 1 && calculatePayloadSize(data) > MAX_PAYLOAD_BYTES) {
                    items.remove(0);
                    if (!msgs.isEmpty()) {
                        msgs.remove(0);
                    }
                    data.put("items", objectMapper.writeValueAsString(items));
                    if (msgsJson != null) {
                        data.put("msgs", objectMapper.writeValueAsString(msgs));
                    }
                }

                if (calculatePayloadSize(data) <= MAX_PAYLOAD_BYTES) {
                    return data;
                }
            } catch (Exception e) {
                logger.warn("[PUSH] Error parsing items during fitToLimit: {}", e.getMessage());
            }
        }

        // Attempt 2: Truncate preview field
        String preview = data.get("preview");
        if (preview != null && preview.length() > 50) {
            data.put("preview", preview.substring(0, 50));
            if (calculatePayloadSize(data) <= MAX_PAYLOAD_BYTES) {
                return data;
            }
        }

        // Attempt 3: Truncate newest item preview to 50 chars
        if (data.containsKey("items")) {
            try {
                List<Map<String, String>> items = objectMapper.readValue(data.get("items"), new TypeReference<List<Map<String, String>>>() {});
                if (!items.isEmpty()) {
                    Map<String, String> last = items.get(items.size() - 1);
                    String x = last.get("x");
                    if (x != null && x.length() > 50) {
                        last.put("x", x.substring(0, 50));
                    }
                    data.put("items", objectMapper.writeValueAsString(items));
                }
            } catch (Exception ignored) {}
        }

        return data;
    }

    /**
     * Constructs WebpushConfig with high or normal urgency and 24h TTL.
     * Crucial: DOES NOT set any notification block.
     */
    public static WebpushConfig buildWebpushConfig(String urgency) {
        return WebpushConfig.builder()
                .putHeader("TTL", "86400")
                .putHeader("Urgency", urgency != null ? urgency : "high")
                .build();
    }

    // =========================================================================
    // 1. CHAT PAYLOAD & MESSAGE
    // =========================================================================
    public static Map<String, String> buildChatData(
            String chatId,
            String legacyTitle,
            String chatName,
            String chatKind,
            String alertKind,
            String senderId,
            String senderName,
            String msgType,
            String msgId,
            String spaceId,
            String preview,
            List<Map<String, String>> items,
            int newCount,
            int unread,
            long ts,
            boolean isMention,
            boolean isReplyToUser,
            List<String> legacyMsgs,
            String chatType,
            String repliedTo
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("v", "2");
        data.put("type", "CHAT");
        data.put("chatId", defaultString(chatId));
        data.put("title", defaultString(legacyTitle));
        data.put("chatName", defaultString(chatName));
        data.put("chatKind", defaultString(chatKind, "group"));
        data.put("alertKind", defaultString(alertKind, "message"));
        data.put("senderId", defaultString(senderId));
        data.put("senderName", defaultString(senderName));
        data.put("msgType", defaultString(msgType, "text"));
        data.put("msgId", defaultString(msgId));
        data.put("spaceId", defaultString(spaceId, "0"));
        data.put("preview", sanitizePreview(preview));

        String itemsJson = "[]";
        if (items != null && !items.isEmpty()) {
            try {
                itemsJson = objectMapper.writeValueAsString(items);
            } catch (Exception e) {
                logger.error("[PUSH] Error serializing items: {}", e.getMessage());
            }
        }
        data.put("items", itemsJson);

        data.put("newCount", String.valueOf(newCount));
        data.put("unread", String.valueOf(unread));
        data.put("ts", String.valueOf(ts > 0 ? ts : System.currentTimeMillis()));
        data.put("isMention", isMention ? "true" : "false");
        data.put("isReplyToMe", isReplyToUser ? "true" : "false");

        String msgsJson = "[]";
        if (legacyMsgs != null && !legacyMsgs.isEmpty()) {
            try {
                msgsJson = objectMapper.writeValueAsString(legacyMsgs);
            } catch (Exception e) {
                logger.error("[PUSH] Error serializing legacy msgs: {}", e.getMessage());
            }
        }
        data.put("msgs", msgsJson);

        data.put("chatType", defaultString(chatType, "group"));
        data.put("repliedTo", defaultString(repliedTo));

        return data;
    }

    public static Message buildChatMessage(Map<String, String> data, String token) {
        Map<String, String> finalData = fitToLimit(data);
        return Message.builder()
                .setToken(token)
                .putAllData(finalData)
                .setWebpushConfig(buildWebpushConfig("high"))
                .build();
    }

    // =========================================================================
    // 2. EVENT REMINDER PAYLOAD & MESSAGE
    // =========================================================================
    public static Map<String, String> buildEventReminderData(
            String eventId,
            String eventTitle,
            String chatId,
            String chatName,
            String startTime,
            int minutesBefore,
            String location,
            String meetingType,
            String meetingLink,
            String legacyTitle
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("v", "2");
        data.put("type", "EVENT");
        data.put("action", "reminder");
        data.put("eventId", defaultString(eventId));
        data.put("eventTitle", defaultString(eventTitle));
        data.put("chatId", defaultString(chatId));
        data.put("chatName", defaultString(chatName));
        data.put("startTime", defaultString(startTime));
        data.put("minutesBefore", String.valueOf(minutesBefore));
        data.put("location", defaultString(location));
        data.put("meetingType", defaultString(meetingType, "offline"));
        data.put("meetingLink", defaultString(meetingLink));
        data.put("title", defaultString(legacyTitle));
        return data;
    }

    public static Message buildEventReminderMessage(Map<String, String> data, String token) {
        Map<String, String> finalData = fitToLimit(data);
        return Message.builder()
                .setToken(token)
                .putAllData(finalData)
                .setWebpushConfig(buildWebpushConfig("high"))
                .build();
    }

    // =========================================================================
    // 3. EVENT CHANGED PAYLOAD & MESSAGE
    // =========================================================================
    public static Map<String, String> buildEventChangedData(
            String action,
            String eventId,
            String eventTitle,
            String chatId,
            String chatName,
            String startTime,
            List<Map<String, String>> changesList,
            String personName,
            String location,
            String legacyTitle,
            String changeSummary
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("v", "2");
        data.put("type", "EVENT_CHANGED");
        data.put("action", defaultString(action, "updated"));
        data.put("eventId", defaultString(eventId));
        data.put("eventTitle", defaultString(eventTitle));
        data.put("chatId", defaultString(chatId));
        data.put("chatName", defaultString(chatName));
        data.put("startTime", defaultString(startTime));

        String changesJson = "[]";
        if (changesList != null && !changesList.isEmpty()) {
            try {
                changesJson = objectMapper.writeValueAsString(changesList);
            } catch (Exception e) {
                logger.error("[PUSH] Error serializing changesList: {}", e.getMessage());
            }
        }
        data.put("changes", changesJson);

        data.put("personName", defaultString(personName));
        data.put("location", defaultString(location));
        data.put("changeSummary", defaultString(changeSummary));
        data.put("title", defaultString(legacyTitle));
        return data;
    }

    public static Message buildEventChangedMessage(Map<String, String> data, String token) {
        Map<String, String> finalData = fitToLimit(data);
        return Message.builder()
                .setToken(token)
                .putAllData(finalData)
                .setWebpushConfig(buildWebpushConfig("high"))
                .build();
    }

    // =========================================================================
    // 4. DAILY SUMMARY PAYLOAD & MESSAGE
    // =========================================================================
    public static Map<String, String> buildDailySummaryData(
            int total,
            List<Map<String, String>> eventsV2List,
            List<Long> legacyEventIds,
            String date
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("v", "2");
        data.put("type", "DAILY");
        data.put("total", String.valueOf(total));

        String eventsV2Json = "[]";
        if (eventsV2List != null && !eventsV2List.isEmpty()) {
            try {
                eventsV2Json = objectMapper.writeValueAsString(eventsV2List);
            } catch (Exception e) {
                logger.error("[PUSH] Error serializing eventsV2List: {}", e.getMessage());
            }
        }
        data.put("eventsV2", eventsV2Json);

        String legacyEventsJson = "[]";
        if (legacyEventIds != null && !legacyEventIds.isEmpty()) {
            try {
                legacyEventsJson = objectMapper.writeValueAsString(legacyEventIds);
            } catch (Exception e) {
                logger.error("[PUSH] Error serializing legacyEventIds: {}", e.getMessage());
            }
        }
        data.put("events", legacyEventsJson);

        data.put("date", defaultString(date));
        return data;
    }

    public static Message buildDailySummaryMessage(Map<String, String> data, String token) {
        Map<String, String> finalData = fitToLimit(data);
        return Message.builder()
                .setToken(token)
                .putAllData(finalData)
                .setWebpushConfig(buildWebpushConfig("normal"))
                .build();
    }

    // =========================================================================
    // 5. MISSING ASSIGNMENT PAYLOAD & MESSAGE
    // =========================================================================
    public static Map<String, String> buildMissingAssignmentData(
            String userId,
            int total,
            List<Map<String, String>> assignmentsV2List,
            List<Long> legacyAssignmentIds
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("v", "2");
        data.put("type", "ASSIGNMENT_MISSING");
        data.put("total", String.valueOf(total));

        String assignmentsV2Json = "[]";
        if (assignmentsV2List != null && !assignmentsV2List.isEmpty()) {
            try {
                assignmentsV2Json = objectMapper.writeValueAsString(assignmentsV2List);
            } catch (Exception e) {
                logger.error("[PUSH] Error serializing assignmentsV2List: {}", e.getMessage());
            }
        }
        data.put("assignmentsV2", assignmentsV2Json);

        String legacyAssignmentsJson = "[]";
        if (legacyAssignmentIds != null && !legacyAssignmentIds.isEmpty()) {
            try {
                legacyAssignmentsJson = objectMapper.writeValueAsString(legacyAssignmentIds);
            } catch (Exception e) {
                logger.error("[PUSH] Error serializing legacyAssignmentIds: {}", e.getMessage());
            }
        }
        data.put("assignments", legacyAssignmentsJson);

        data.put("userId", defaultString(userId));
        return data;
    }

    public static Message buildMissingAssignmentMessage(Map<String, String> data, String token) {
        Map<String, String> finalData = fitToLimit(data);
        return Message.builder()
                .setToken(token)
                .putAllData(finalData)
                .setWebpushConfig(buildWebpushConfig("normal"))
                .build();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private static String defaultString(String val) {
        return val != null ? val : "";
    }

    private static String defaultString(String val, String def) {
        return (val != null && !val.isEmpty()) ? val : def;
    }
}
