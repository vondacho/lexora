package ch.conciliations.mabecoju.infrastructure.security;

import ch.conciliations.mabecoju.domain.user.Grant;
import ch.conciliations.mabecoju.domain.user.GrantRepository;
import ch.conciliations.mabecoju.domain.user.User;
import ch.conciliations.mabecoju.domain.user.UserRepository;
import ch.conciliations.mabecoju.domain.user.Username;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates against {@code all_users}; the grants of {@code all_grants} become authorities named
 * {@code module:page}.
 */
@Service
class LegacyUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final UserRepository users;
    private final GrantRepository grants;

    LegacyUserDetailsService(UserRepository users, GrantRepository grants) {
        this.users = users;
        this.grants = grants;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Username name;
        try {
            name = Username.of(username);
        } catch (RuntimeException invalid) {
            throw new UsernameNotFoundException("Unknown user");
        }
        User user = users.findByUsername(name).orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return toDetails(user);
    }

    /** Stores the re-hashed password when legacy MD5 hashes are being upgraded. */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails details, String newPassword) {
        User user = users.findByUsername(Username.of(details.getUsername())).orElseThrow();
        user.changePasswordHash(newPassword);
        return toDetails(users.save(user));
    }

    private UserDetails toDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.username().value())
                .password(user.passwordHash() == null ? "" : user.passwordHash())
                .authorities(grants.findByUser(user.username()).stream()
                        .map(Grant::authority)
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
    }
}
