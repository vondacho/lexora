package io.lexora.application.user;

/** Port hashing user passwords before they are stored. */
public interface PasswordHasher {

    String hash(String rawPassword);
}
