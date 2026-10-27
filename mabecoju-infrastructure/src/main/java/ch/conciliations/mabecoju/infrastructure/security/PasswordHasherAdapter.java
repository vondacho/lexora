package ch.conciliations.mabecoju.infrastructure.security;

import ch.conciliations.mabecoju.application.user.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class PasswordHasherAdapter implements PasswordHasher {

    private final PasswordEncoder encoder;

    PasswordHasherAdapter(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
