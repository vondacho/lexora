package io.lexora.infrastructure.branding;

import java.util.Locale;

/** The colours of the brand, written {@code #rrggbb} in lower case. */
final class Colors {

    /** Share of the main colour kept in the dark one; the rest is black. */
    private static final double DARKEN = 0.78;

    private Colors() {
    }

    static String accent(BrandingProperties properties) {
        return properties.accentColor().toLowerCase(Locale.ROOT);
    }

    static String accentDark(BrandingProperties properties) {
        String dark = properties.accentDarkColor();
        return dark == null || dark.isBlank() ? darken(accent(properties)) : dark.toLowerCase(Locale.ROOT);
    }

    static String darken(String color) {
        int rgb = Integer.parseInt(color.substring(1), 16);
        int r = (int) Math.round((rgb >> 16 & 0xff) * DARKEN);
        int g = (int) Math.round((rgb >> 8 & 0xff) * DARKEN);
        int b = (int) Math.round((rgb & 0xff) * DARKEN);
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }
}
