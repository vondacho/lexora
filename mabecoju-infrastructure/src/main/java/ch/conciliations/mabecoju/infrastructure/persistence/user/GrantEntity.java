package ch.conciliations.mabecoju.infrastructure.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "all_grants")
public class GrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gran_id")
    Integer id;

    @Column(name = "gran_user_name", length = 16)
    String username;

    @Column(name = "gran_module_name", length = 20)
    String module;

    @Column(name = "gran_page_name", length = 50)
    String page;
}
