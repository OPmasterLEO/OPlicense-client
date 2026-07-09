package net.opmasterleo.license.internal.json;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SimpleJsonTest {

    @Test
    void objectEscapesWindowsPathsAndControlCharacters() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", 1_700_000_000L);
        fields.put("nonce", "abc123");
        fields.put("hwid", "HWID-test");
        fields.put("userDir", "C:\\Visual Studio Code\\lifesteal");
        fields.put("note", "line1\nline2");

        String json = SimpleJson.object(fields);

        assertEquals(
                "{\"timestamp\":1700000000,\"nonce\":\"abc123\",\"hwid\":\"HWID-test\","
                        + "\"userDir\":\"C:\\\\Visual Studio Code\\\\lifesteal\","
                        + "\"note\":\"line1\\nline2\"}",
                json
        );
        assertFalse(json.contains("C:\\Visual"));
    }
}
