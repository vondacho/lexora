package io.lexora.infrastructure.branding;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The brand is public, has defaults, follows the settings, and wrong settings stop the start. */
class BrandingApiTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    class Defaults {

        @Autowired
        MockMvc mvc;

        @Test
        void the_product_brand_is_public() throws Exception {
            mvc.perform(get("/api/branding"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productName").value("Lexora"))
                    .andExpect(jsonPath("$.officeName").doesNotExist())
                    .andExpect(jsonPath("$.accentColor").value("#1d5fb4"))
                    .andExpect(jsonPath("$.accentDarkColor").value("#174a8c"))
                    .andExpect(jsonPath("$.logoIncludesName").value(false))
                    .andExpect(jsonPath("$.logoUrl", startsWith("/api/branding/logo?v=")));
        }

        @Test
        void without_logo_a_monogram_in_the_main_colour_is_served() throws Exception {
            mvc.perform(get("/api/branding/logo").header("Accept", "image/avif,image/webp,*/*"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/svg+xml"))
                    .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                    .andExpect(content().string(containsString("fill=\"#1d5fb4\"")))
                    .andExpect(content().string(containsString(">L</text>")));
            mvc.perform(get("/api/branding/favicon")).andExpect(status().isOk());
        }

        @Test
        void the_rest_of_the_api_still_needs_credentials() throws Exception {
            mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "lexora.branding.office-name=Étude Exemple",
            "lexora.branding.tagline-fr=juriste & médiatrice",
            "lexora.branding.accent-color=#7A1F5C",
            "lexora.branding.logo=classpath:branding/test-logo.svg",
            "lexora.branding.logo-includes-name=true"})
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    class Configured {

        @Autowired
        MockMvc mvc;

        @Test
        void the_office_brand_is_served() throws Exception {
            mvc.perform(get("/api/branding"))
                    .andExpect(jsonPath("$.officeName").value("Étude Exemple"))
                    .andExpect(jsonPath("$.taglineFr").value("juriste & médiatrice"))
                    .andExpect(jsonPath("$.taglineEn").doesNotExist())
                    .andExpect(jsonPath("$.accentColor").value("#7a1f5c"))
                    .andExpect(jsonPath("$.accentDarkColor").value("#5f1848"))
                    .andExpect(jsonPath("$.logoIncludesName").value(true));
            mvc.perform(get("/api/branding/logo"))
                    .andExpect(content().string(containsString("<circle")));
            mvc.perform(get("/api/branding/favicon"))
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                            .contains(">É</text>"));
        }
    }

    @Configuration
    @EnableConfigurationProperties(BrandingProperties.class)
    @Import(BrandingImages.class)
    static class PropertiesOnly {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesOnly.class);

    @Test
    void a_wrong_colour_stops_the_start() {
        runner.withPropertyValues("lexora.branding.accent-color=blue")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("accentColor"));
    }

    @Test
    void a_missing_logo_stops_the_start() {
        runner.withPropertyValues("lexora.branding.logo=classpath:branding/nowhere.svg")
                .run(context -> assertThat(context.getStartupFailure()).rootCause()
                        .hasMessageContaining("lexora.branding.logo").hasMessageContaining("does not exist"));
    }

    @Test
    void a_logo_that_is_not_an_image_stops_the_start() {
        runner.withPropertyValues("lexora.branding.logo=classpath:branding/notes.txt")
                .run(context -> assertThat(context.getStartupFailure()).rootCause()
                        .hasMessageContaining("must be one of"));
    }
}
