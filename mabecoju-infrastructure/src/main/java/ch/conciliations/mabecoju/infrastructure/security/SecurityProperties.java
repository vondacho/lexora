package ch.conciliations.mabecoju.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param legacyPasswordHashing keep hashing new passwords with unsalted MD5 so that the PHP application can still
 *                              authenticate the same users; set to false once it is retired, passwords are then
 *                              hashed with bcrypt and legacy hashes upgraded at the next login
 * @param allowedOrigins        origins of the web front end allowed to call the API (CORS)
 */
@ConfigurationProperties(prefix = "mabecoju.security")
public record SecurityProperties(Boolean legacyPasswordHashing, List<String> allowedOrigins) {

    public SecurityProperties {
        legacyPasswordHashing = legacyPasswordHashing == null || legacyPasswordHashing;
        allowedOrigins = allowedOrigins == null ? List.of("http://localhost:4200") : List.copyOf(allowedOrigins);
    }
}
