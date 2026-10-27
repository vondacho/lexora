package io.lexora.infrastructure.branding;

import io.lexora.infrastructure.web.api.BrandingApi;
import io.lexora.infrastructure.web.api.model.BrandingDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/** Public: the login page shows the brand before anyone is authenticated (see SecurityConfiguration). */
@RestController
class BrandingController implements BrandingApi {

    private final BrandingProperties properties;
    private final BrandingImages images;

    BrandingController(BrandingProperties properties, BrandingImages images) {
        this.properties = properties;
        this.images = images;
    }

    @Override
    public BrandingDto getBranding() {
        return new BrandingDto(properties.productName(), Colors.accent(properties), Colors.accentDark(properties),
                "/api/branding/logo?v=" + images.logo().version(), properties.logoIncludesName(),
                "/api/branding/favicon?v=" + images.favicon().version())
                .officeName(blankToNull(properties.officeName()))
                .taglineFr(blankToNull(properties.taglineFr()))
                .taglineEn(blankToNull(properties.taglineEn()));
    }

    @Override
    public Resource getBrandingLogo() {
        return send(images.logo());
    }

    @Override
    public Resource getBrandingFavicon() {
        return send(images.favicon());
    }

    /**
     * The type is set on the response itself: content negotiation would otherwise pick the first type of the
     * contract that the browser accepts. The URLs carry the image's version, so the image can be kept a day.
     */
    private static Resource send(BrandingImages.Image image) {
        var response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
        if (response != null) {
            response.setContentType(image.type().toString());
            response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(Duration.ofDays(1)).cachePublic()
                    .getHeaderValue());
            response.setHeader("X-Content-Type-Options", "nosniff");
        }
        return new ByteArrayResource(image.bytes());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
