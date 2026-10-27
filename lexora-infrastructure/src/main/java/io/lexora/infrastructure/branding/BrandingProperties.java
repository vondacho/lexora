package io.lexora.infrastructure.branding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * Brand of this installation, shown by the front end (also on the login page) and written in the PDFs' metadata.
 * Each property can be given as an environment variable, e.g. {@code LEXORA_BRANDING_OFFICE_NAME}.
 *
 * @param productName      name of the product, "Lexora"
 * @param officeName       name of the office; the product name is shown when absent
 * @param taglineFr        line under the name, in French
 * @param taglineEn        the same in English; the French one when absent
 * @param accentColor      main colour, {@code #rrggbb}
 * @param accentDarkColor  hover and pressed colour; derived from the main colour when absent
 * @param logo             SVG, PNG, JPEG or WebP image ({@code file:/etc/lexora/logo.svg}); a monogram in the
 *                         main colour when absent
 * @param logoIncludesName the logo already shows the name, so the header does not repeat it
 * @param favicon          ICO, SVG or PNG icon of the browser tab; the monogram when absent
 */
@Validated
@ConfigurationProperties(prefix = "lexora.branding")
public record BrandingProperties(
        @DefaultValue("Lexora") @NotBlank String productName,
        String officeName,
        String taglineFr,
        String taglineEn,
        @DefaultValue("#1d5fb4") @Pattern(regexp = COLOR, message = "must be a colour like #1d5fb4") String accentColor,
        @Pattern(regexp = COLOR, message = "must be a colour like #164a8c") String accentDarkColor,
        Resource logo,
        boolean logoIncludesName,
        Resource favicon) {

    static final String COLOR = "#[0-9a-fA-F]{6}";

    /** The office name, or the product name for an installation without one. */
    public String displayName() {
        return officeName == null || officeName.isBlank() ? productName : officeName.strip();
    }
}
