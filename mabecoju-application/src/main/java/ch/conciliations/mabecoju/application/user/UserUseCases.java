package ch.conciliations.mabecoju.application.user;

import ch.conciliations.mabecoju.application.common.Ids;
import ch.conciliations.mabecoju.domain.shared.DomainException;
import ch.conciliations.mabecoju.domain.shared.ErrorCode;
import ch.conciliations.mabecoju.domain.shared.Strings;
import ch.conciliations.mabecoju.domain.user.Grant;
import ch.conciliations.mabecoju.domain.user.GrantCatalog;
import ch.conciliations.mabecoju.domain.user.GrantRepository;
import ch.conciliations.mabecoju.domain.user.User;
import ch.conciliations.mabecoju.domain.user.UserRepository;
import ch.conciliations.mabecoju.domain.user.Username;
import jakarta.transaction.Transactional;

import java.util.List;

/** Administrating users and their grants. */
@Transactional
public class UserUseCases {

    private final UserRepository users;
    private final GrantRepository grants;
    private final PasswordHasher passwordHasher;

    public UserUseCases(UserRepository users, GrantRepository grants, PasswordHasher passwordHasher) {
        this.users = users;
        this.grants = grants;
        this.passwordHasher = passwordHasher;
    }

    public UserAccount createUser(Username username, String password, String firstName, String lastName,
                                  String shortcut) {
        if (users.findByUsername(username).isPresent()) {
            throw new DomainException(ErrorCode.USER_EXISTS, "User " + username.value() + " already exists")
                    .with("username", username.value());
        }
        Strings.requireNotBlank(password, "Password");
        User user = users.save(User.register(username, passwordHasher.hash(password), firstName, lastName, shortcut));
        return new UserAccount(user, grants.findByUser(username));
    }

    /** @param newPassword null or blank to keep the current password */
    public UserAccount updateUser(Username username, String newPassword, String firstName, String lastName,
                                  String shortcut) {
        User user = load(username);
        user.updateProfile(firstName, lastName, shortcut);
        if (!Strings.isBlank(newPassword)) {
            user.changePasswordHash(passwordHasher.hash(newPassword));
        }
        return new UserAccount(users.save(user), grants.findByUser(username));
    }

    public void deleteUser(Username username) {
        load(username);
        grants.removeAll(username);
        users.delete(username);
    }

    public UserAccount getUser(Username username) {
        return new UserAccount(load(username), grants.findByUser(username));
    }

    public List<User> listUsers() {
        return users.findAll();
    }

    public List<Grant> grantCatalog() {
        return GrantCatalog.ALL;
    }

    public UserAccount grant(Username username, Grant grant) {
        load(username);
        if (!GrantCatalog.contains(grant)) {
            throw new DomainException(ErrorCode.UNKNOWN_GRANT, "Unknown grant " + grant.authority())
                    .with("grant", grant.authority());
        }
        if (!grants.findByUser(username).contains(grant)) {
            grants.add(username, grant);
        }
        return getUser(username);
    }

    public UserAccount revoke(Username username, Grant grant) {
        load(username);
        grants.remove(username, grant);
        return getUser(username);
    }

    public UserAccount grantAll(Username username) {
        load(username);
        var current = grants.findByUser(username);
        GrantCatalog.ALL.stream().filter(g -> !current.contains(g)).forEach(g -> grants.add(username, g));
        return getUser(username);
    }

    public UserAccount revokeAll(Username username) {
        load(username);
        grants.removeAll(username);
        return getUser(username);
    }

    private User load(Username username) {
        return Ids.require(users.findByUsername(username), "User", username.value());
    }
}
