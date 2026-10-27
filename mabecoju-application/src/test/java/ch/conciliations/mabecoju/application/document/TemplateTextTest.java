package ch.conciliations.mabecoju.application.document;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateTextTest {

    @Test
    void fills_legacy_php_style_placeholders_with_escaped_values() {
        String text = "$title_1, ${title_2},\nmontant de frs. $amount.- au $date; inconnu: [$nope]";

        String filled = TemplateText.fill(text, Map.of("title_1", "Madame", "title_2", "Monsieur",
                "amount", "1250.00", "date", "24 octobre 2026"));

        assertThat(filled).isEqualTo("Madame, Monsieur,\nmontant de frs. 1250.00.- au 24 octobre 2026; inconnu: []");
        assertThat(TemplateText.fill("$name", Map.of("name", "A & <B>"))).isEqualTo("A &amp; &lt;B&gt;");
    }

    @Test
    void recognises_html_templates() {
        assertThat(TemplateText.isHtml("<p>Madame,</p>")).isTrue();
        assertThat(TemplateText.isHtml("Madame,\nfrs. 300 < 400")).isFalse();
    }

    @Test
    void formats_like_the_legacy_documents() {
        assertThat(DocumentFormats.longDate(java.time.LocalDate.of(2026, 2, 1))).isEqualTo("1 février 2026");
        assertThat(DocumentFormats.amount(ch.conciliations.mabecoju.domain.shared.Money.of("1250.5"))).isEqualTo("1250.50");
    }
}
