package io.lexora.infrastructure.persistence.template;

import io.lexora.infrastructure.persistence.support.AuditListener;
import io.lexora.infrastructure.persistence.support.Audited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "invo_models")
@EntityListeners(AuditListener.class)
public class DocumentModelEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mode_id")
    Integer id;

    @Column(name = "mode_name", length = 80, unique = true)
    String name;

    @Column(name = "mode_title", length = 80)
    String title;

    @Column(name = "mode_content", columnDefinition = "text", nullable = false)
    String content;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "mode_adr_count", nullable = false)
    Integer addressCount;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "mode_type", nullable = false)
    Integer type;

    @Column(name = "mode_layout_1", length = 50)
    String layout1;

    @Column(name = "mode_layout_2", length = 50)
    String layout2;

    @Column(name = "mode_layout_3", length = 50)
    String layout3;

    @Column(name = "mode_layout_4", length = 50)
    String layout4;

    @Column(name = "mode_single_page_1")
    Boolean singlePage1;

    @Column(name = "mode_single_page_2")
    Boolean singlePage2;

    @Column(name = "mode_single_page_3")
    Boolean singlePage3;

    @Column(name = "mode_single_page_4")
    Boolean singlePage4;

    @Column(name = "mode_mute_date")
    LocalDate muteDate;

    @Column(name = "mode_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
