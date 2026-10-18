package com.letschat.mvp_1.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UsernameSimilarityEvaluator {

    public static final double FLOOR = 0.50;
    public static final double BLOCK_THRESHOLD = 8.0;
    public static final double WARN_THRESHOLD = 6.0;

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
        "letschat",
        "admin",
        "administrator",
        "system",
        "sysadmin",
        "server",
        "root",
        "support",
        "helpdesk",
        "moderator",
        "official",
        "security",
        "bot"
    );

    /**
     * Checks if a name contains or mimics reserved system / admin names
     * such as letschat, admin, system, server, etc.
     */
    public static boolean isReserved(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String rawLower = name.trim().toLowerCase();
        String norm = normalize(name);

        // 1. Direct contains for primary brand name
        if (norm.contains("letschat")) {
            return true;
        }

        // 2. Exact match against reserved set
        if (RESERVED_KEYWORDS.contains(norm) || RESERVED_KEYWORDS.contains(rawLower)) {
            return true;
        }

        // 3. Strip trailing numbers (e.g. admin123, system01, server99)
        String noDigits = norm.replaceAll("\\d+$", "");
        if (!noDigits.isEmpty() && RESERVED_KEYWORDS.contains(noDigits)) {
            return true;
        }

        // 4. Token-based check (splitting on non-alphanumeric delimiters like _, ., -, space)
        String[] tokens = rawLower.split("[^a-z0-9]+");
        for (String token : tokens) {
            String normToken = normalize(token);
            String normTokenNoDigits = normToken.replaceAll("\\d+$", "");
            if (RESERVED_KEYWORDS.contains(normToken) || (!normTokenNoDigits.isEmpty() && RESERVED_KEYWORDS.contains(normTokenNoDigits))) {
                return true;
            }
        }

        return false;
    }

    /**
     * STEP 1: Normalize
     * 1. Convert to lowercase.
     * 2. Replace lookalikes: 0->o, 1->i, 3->e, 4->a, 5->s, 7->t, @->a, $->s
     * 3. Remove everything that is not a-z or 0-9.
     */
    public static String normalize(String name) {
        if (name == null) return "";
        String s = name.toLowerCase().trim();

        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '0': sb.append('o'); break;
                case '1': sb.append('i'); break;
                case '3': sb.append('e'); break;
                case '4': sb.append('a'); break;
                case '5': sb.append('s'); break;
                case '7': sb.append('t'); break;
                case '@': sb.append('a'); break;
                case '$': sb.append('s'); break;
                default:
                    if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Computes similarity score between two normalized names (0.0 to 10.0).
     */
    public static double computeScore(String normA, String normB) {
        if (normA == null || normB == null || normA.isEmpty() || normB.isEmpty()) {
            return 0.0;
        }

        // STEP 2: Exact Match
        if (normA.equals(normB)) {
            return 10.0;
        }

        int lenA = normA.length();
        int lenB = normB.length();
        double L = Math.max(lenA, lenB);
        if (L == 0) return 0.0;

        // STEP 3: THREE MEASURES
        // 1. editSim
        int levDist = levenshteinDistance(normA, normB);
        double editSim = 1.0 - ((double) levDist / L);
        editSim = Math.max(0.0, Math.min(1.0, editSim));

        // 2. bigramSim (Dice coefficient with multiplicity)
        double bigramSim = diceBigramSimilarity(normA, normB);

        // 3. runSim (Longest Common Substring length / L)
        int lcsLen = longestCommonSubstring(normA, normB);
        double runSim = (double) lcsLen / L;
        runSim = Math.max(0.0, Math.min(1.0, runSim));

        // STEP 4: COMBINE WITH OVERLAP FLOOR
        double raw = (editSim + bigramSim + runSim) / 3.0;

        if (raw <= FLOOR) {
            return 0.0;
        }

        double score = ((raw - FLOOR) / (1.0 - FLOOR)) * 10.0;
        score = Math.min(score, 9.9);
        return Math.round(score * 10.0) / 10.0;
    }

    /**
     * Standard Levenshtein Distance
     */
    public static int levenshteinDistance(String a, String b) {
        int m = a.length();
        int n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            char cA = a.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                char cB = b.charAt(j - 1);
                int cost = (cA == cB) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[n];
    }

    /**
     * Dice coefficient on two-letter chunks with multiplicity
     */
    public static double diceBigramSimilarity(String a, String b) {
        if (a.length() < 2 || b.length() < 2) {
            return 0.0;
        }

        Map<String, Integer> bigramsA = getBigramCounts(a);
        Map<String, Integer> bigramsB = getBigramCounts(b);

        int totalA = a.length() - 1;
        int totalB = b.length() - 1;

        int shared = 0;
        for (Map.Entry<String, Integer> entry : bigramsA.entrySet()) {
            String bigram = entry.getKey();
            int countInB = bigramsB.getOrDefault(bigram, 0);
            shared += Math.min(entry.getValue(), countInB);
        }

        return (2.0 * shared) / (totalA + totalB);
    }

    private static Map<String, Integer> getBigramCounts(String s) {
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < s.length() - 1; i++) {
            String bg = s.substring(i, i + 2);
            counts.put(bg, counts.getOrDefault(bg, 0) + 1);
        }
        return counts;
    }

    /**
     * Longest Common Substring length
     */
    public static int longestCommonSubstring(String a, String b) {
        int m = a.length();
        int n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        int maxLen = 0;

        for (int i = 1; i <= m; i++) {
            char cA = a.charAt(i - 1);
            for (int j = 1; j <= n; j++) {
                char cB = b.charAt(j - 1);
                if (cA == cB) {
                    curr[j] = prev[j - 1] + 1;
                    if (curr[j] > maxLen) {
                        maxLen = curr[j];
                    }
                } else {
                    curr[j] = 0;
                }
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return maxLen;
    }
}
