package io.lexora.infrastructure.branding;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Logo and favicon of the installation, read once at startup: a configured image that is missing or of another
 * type stops the start with a clear message rather than showing a broken image later.
 */
@Component
class BrandingImages {

    private static final Set<MediaType> LOGO_TYPES = Set.of(MediaType.valueOf("image/svg+xml"), MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG, MediaType.valueOf("image/webp"));
    private static final Set<MediaType> FAVICON_TYPES = Set.of(MediaType.valueOf("image/x-icon"),
            MediaType.valueOf("image/vnd.microsoft.icon"), MediaType.valueOf("image/svg+xml"), MediaType.IMAGE_PNG);

    private final Image logo;
    private final Image favicon;

    BrandingImages(BrandingProperties properties) {
        Image monogram = monogram(properties.displayName(), Colors.accent(properties));
        this.logo = properties.logo() == null ? monogram : read(properties.logo(), LOGO_TYPES, "logo");
        this.favicon = properties.favicon() == null ? monogram : read(properties.favicon(), FAVICON_TYPES, "favicon");
    }

    Image logo() {
        return logo;
    }

    Image favicon() {
        return favicon;
    }

    /** Bytes of an image, its type, and a version that changes with its content (for long caching). */
    record Image(byte[] bytes, MediaType type, String version) {

        static Image of(byte[] bytes, MediaType type) {
            CRC32 crc = new CRC32();
            crc.update(bytes);
            return new Image(bytes, type, HexFormat.of().toHexDigits((int) crc.getValue()));
        }
    }

    private static Image read(Resource resource, Set<MediaType> allowed, String property) {
        String where = "lexora.branding." + property + " (" + resource.getDescription() + ")";
        if (!resource.exists()) {
            throw new IllegalStateException(where + " does not exist");
        }
        MediaType type = MediaTypeFactory.getMediaType(resource)
                .filter(allowed::contains)
                .orElseThrow(() -> new IllegalStateException(where + " must be one of " + allowed));
        try (var in = resource.getInputStream()) {
            return Image.of(in.readAllBytes(), type);
        } catch (IOException e) {
            throw new IllegalStateException(where + " cannot be read", e);
        }
    }

    /** First letter of the name, white on the main colour. */
    static Image monogram(String name, String color) {
        String trimmed = name.strip();
        String letter = trimmed.isEmpty() ? "L"
                : new String(Character.toChars(trimmed.codePointAt(0))).toUpperCase(Locale.ROOT);
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">\
                <rect width="64" height="64" rx="14" fill="%s"/>\
                <text x="32" y="45" text-anchor="middle" fill="#ffffff" font-size="38" font-weight="600" \
                font-family="IBM Plex Sans, system-ui, -apple-system, Segoe UI, sans-serif">%s</text></svg>"""
                .formatted(color, HtmlUtils.htmlEscape(letter, "UTF-8")); // XML: no named entities
        return Image.of(svg.getBytes(StandardCharsets.UTF_8), MediaType.valueOf("image/svg+xml"));
    }
}
