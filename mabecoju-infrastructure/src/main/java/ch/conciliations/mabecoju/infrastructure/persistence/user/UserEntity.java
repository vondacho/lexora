package ch.conciliations.mabecoju.infrastructure.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "all_users")
public class UserEntity {

    @Id
    @Column(name = "user_name", length = 16)
    String username;

    /** Legacy unsalted MD5 hex digest, or a "{id}hash" value written by this application. */
    @Column(name = "user_pswd", length = 80)
    String password;

    @Column(name = "user_first_name", length = 20)
    String firstName;

    @Column(name = "user_last_name", length = 30)
    String lastName;

    @Column(name = "user_shortcut", length = 3)
    String shortcut;
}
