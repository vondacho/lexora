package ch.conciliations.mabecoju.domain.user;

import ch.conciliations.mabecoju.domain.shared.Strings;

import java.util.Objects;

/** A person allowed to use the application ({@code all_users}). */
public final class User {

    private final Username username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String shortcut;

    private User(Username username, String passwordHash, String firstName, String lastName, String shortcut) {
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = passwordHash;
        updateProfile(firstName, lastName, shortcut);
    }

    public static User register(Username username, String passwordHash, String firstName, String lastName,
                                String shortcut) {
        Strings.requireNotBlank(shortcut, "Shortcut");
        return new User(username, passwordHash, firstName, lastName, shortcut);
    }

    public static User restore(Username username, String passwordHash, String firstName, String lastName,
                               String shortcut) {
        return new User(username, passwordHash, firstName, lastName, shortcut);
    }

    public void updateProfile(String firstName, String lastName, String shortcut) {
        this.firstName = Strings.requireMaxLength(firstName, 20, "First name");
        this.lastName = Strings.requireMaxLength(lastName, 30, "Last name");
        this.shortcut = Strings.requireMaxLength(shortcut, 3, "Shortcut");
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String fullName() {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    public Username username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String shortcut() {
        return shortcut;
    }
}
