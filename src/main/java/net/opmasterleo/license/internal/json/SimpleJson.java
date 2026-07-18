package net.opmasterleo.license.internal.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SimpleJson {

    private SimpleJson() {
    }

    public static String object(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value.toString());
            } else {
                sb.append("\"").append(escape(value.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static final Pattern FLAT_PATTERN = Pattern.compile(
            "\"(\\w+)\"\\s*:\\s*(\"((?:\\\\.|[^\"\\\\])*)\"|true|false|null|[-0-9.]+)"
    );

    public static Map<String, String> parseFlat(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        Matcher matcher = FLAT_PATTERN.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(3) != null ? matcher.group(3) : matcher.group(2);
            result.put(key, value);
        }
        return result;
    }

    public static String[] parseStringArray(String json, String key) {
        Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]");
        Matcher fieldMatcher = fieldPattern.matcher(json);
        if (!fieldMatcher.find()) return new String[0];

        String inner = fieldMatcher.group(1).trim();
        if (inner.isEmpty()) return new String[0];

        Matcher valueMatcher = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(inner);
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
        }
        return values.toArray(new String[0]);
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                    break;
            }
        }
        return escaped.toString();
    }
}
