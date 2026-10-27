package ch.conciliations.mabecoju.application.document;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills the placeholders of a letter template. The syntax is the one of the legacy templates, which were PHP
 * strings: {@code $name} or {@code ${name}}. Unknown placeholders are replaced by nothing, as PHP did.
 */
public final class TemplateText {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}|\\$([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern HTML_TAG = Pattern.compile("<(p|br|div|span|b|strong|i|em|u|ul|ol|li|table|h[1-6])\\b",
            Pattern.CASE_INSENSITIVE);

    private TemplateText() {
    }

    /** Replaces the placeholders by their HTML-escaped values. */
    public static String fill(String text, Map<String, String> values) {
        if (text == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(result, Matcher.quoteReplacement(escape(values.getOrDefault(name, ""))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Templates written before the HTML editor are plain text. */
    public static boolean isHtml(String text) {
        return text != null && HTML_TAG.matcher(text).find();
    }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
