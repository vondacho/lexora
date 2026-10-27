package ch.conciliations.mabecoju.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Understands the unsalted MD5 hex digests written by the PHP application as well as {@code {bcrypt}} hashes.
 * <p>
 * Unlike the legacy application, an empty stored password never matches.
 */
public class LegacyAwarePasswordEncoder implements PasswordEncoder {

    private static final String BCRYPT_PREFIX = "{bcrypt}";

    private final boolean legacyHashing;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public LegacyAwarePasswordEncoder(boolean legacyHashing) {
        this.legacyHashing = legacyHashing;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return legacyHashing ? md5(rawPassword) : BCRYPT_PREFIX + bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || rawPassword.isEmpty() || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        if (encodedPassword.startsWith(BCRYPT_PREFIX)) {
            return bcrypt.matches(rawPassword, encodedPassword.substring(BCRYPT_PREFIX.length()));
        }
        return MessageDigest.isEqual(md5(rawPassword).getBytes(StandardCharsets.US_ASCII),
                encodedPassword.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return !legacyHashing && encodedPassword != null && !encodedPassword.startsWith(BCRYPT_PREFIX);
    }

    static String md5(CharSequence rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }
}
