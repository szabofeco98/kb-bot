package com.szabofeco.kb_bot.ingestion;

import java.util.LinkedHashMap;
import java.util.Map;

/** Flat "key: value" frontmatter + body. Intentionally not a full YAML parser. */
record ParsedMarkdown(Map<String, String> frontMatter, String body) {

    static ParsedMarkdown parse(String content) {
        String text = content.replace("\r\n", "\n");
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        if (!text.startsWith("---\n")) {
            return new ParsedMarkdown(Map.of(), text);
        }

        int end = text.indexOf("\n---", 4);
        if (end < 0) {
            throw new InvalidDocumentException("The frontmatter is not closed (missing closing ---).");
        }

        Map<String, String> frontMatter = new LinkedHashMap<>();
        for (String line : text.substring(4, end).split("\n")) {
            int colon = line.indexOf(':');
            if (line.isBlank() || colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = stripQuotes(line.substring(colon + 1).trim());
            frontMatter.put(key, value);
        }

        int bodyStart = text.indexOf('\n', end + 1);
        String body = bodyStart < 0 ? "" : text.substring(bodyStart + 1);
        return new ParsedMarkdown(frontMatter, body);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
