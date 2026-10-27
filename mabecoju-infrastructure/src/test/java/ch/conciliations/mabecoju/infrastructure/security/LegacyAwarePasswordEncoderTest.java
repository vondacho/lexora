package ch.conciliations.mabecoju.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyAwarePasswordEncoderTest {

    private static final String MD5_OF_SECRET = "5ebe2294ecd0e0f08eab7690d2a6ee69";

    @Test
    void accepts_legacy_md5_hashes() {
        LegacyAwarePasswordEncoder encoder = new LegacyAwarePasswordEncoder(true);

        assertThat(encoder.matches("secret", MD5_OF_SECRET)).isTrue();
        assertThat(encoder.matches("secret", MD5_OF_SECRET.toUpperCase())).isTrue();
        assertThat(encoder.matches("wrong", MD5_OF_SECRET)).isFalse();
        assertThat(encoder.encode("secret")).isEqualTo(MD5_OF_SECRET);
        assertThat(encoder.upgradeEncoding(MD5_OF_SECRET)).isFalse();
    }

    @Test
    void never_accepts_empty_passwords_unlike_the_legacy_application() {
        LegacyAwarePasswordEncoder encoder = new LegacyAwarePasswordEncoder(true);

        assertThat(encoder.matches("", "")).isFalse();
        assertThat(encoder.matches("anything", "")).isFalse();
    }

    @Test
    void hashes_with_bcrypt_and_upgrades_md5_once_legacy_hashing_is_off() {
        LegacyAwarePasswordEncoder encoder = new LegacyAwarePasswordEncoder(false);

        String hash = encoder.encode("secret");

        assertThat(hash).startsWith("{bcrypt}");
        assertThat(encoder.matches("secret", hash)).isTrue();
        assertThat(encoder.matches("secret", MD5_OF_SECRET)).isTrue();
        assertThat(encoder.upgradeEncoding(MD5_OF_SECRET)).isTrue();
        assertThat(encoder.upgradeEncoding(hash)).isFalse();
    }
}
