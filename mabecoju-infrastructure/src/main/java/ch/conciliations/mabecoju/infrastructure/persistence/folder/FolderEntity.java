package ch.conciliations.mabecoju.infrastructure.persistence.folder;

import ch.conciliations.mabecoju.infrastructure.persistence.support.AuditListener;
import ch.conciliations.mabecoju.infrastructure.persistence.support.Audited;
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
@Table(name = "invo_folders")
@EntityListeners(AuditListener.class)
public class FolderEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fold_id")
    Integer id;

    @Column(name = "fold_date")
    LocalDate date;

    @Column(name = "fold_name", length = 50)
    String name;

    @Column(name = "fold_path", length = 128)
    String path;

    @Column(name = "fold_status", length = 80)
    String status;

    @Column(name = "fold_number")
    Integer number;

    @Column(name = "fold_first_date")
    LocalDate firstDate;

    @Column(name = "fold_handled_by", length = 10)
    String handledBy;

    @Column(name = "fold_comment", columnDefinition = "text")
    String comment;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "fold_type", nullable = false)
    Integer type;

    @Column(name = "fold_mute_user", length = 10)
    String muteUser;

    @Column(name = "fold_mute_date")
    LocalDate muteDate;

    @Column(name = "fold_wayToOurOffice", columnDefinition = "text")
    String wayToOurOffice;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
