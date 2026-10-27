package ch.conciliations.mabecoju.infrastructure.persistence.customer;

import ch.conciliations.mabecoju.infrastructure.persistence.support.AuditListener;
import ch.conciliations.mabecoju.infrastructure.persistence.support.Audited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "invo_customers")
@EntityListeners(AuditListener.class)
public class CustomerEntity implements Audited {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cust_id")
    Integer id;

    @Column(name = "cust_folder_id", nullable = false)
    Integer folderId;

    @Column(name = "cust_title", length = 20)
    String title;

    @Column(name = "cust_first_name", length = 50)
    String firstName;

    @Column(name = "cust_last_name", length = 50)
    String lastName;

    @Column(name = "cust_address", length = 128)
    String address;

    @Column(name = "cust_city", length = 30)
    String city;

    @Column(name = "cust_zip", length = 10)
    String zip;

    @Column(name = "cust_priv_phone", length = 20)
    String privatePhone;

    @Column(name = "cust_prof_phone", length = 20)
    String professionalPhone;

    @Column(name = "cust_mob_phone", length = 20)
    String mobilePhone;

    @Column(name = "cust_mob_phone2", length = 20)
    String mobilePhone2;

    @Column(name = "cust_email", length = 50)
    String email;

    @Column(name = "cust_email2", length = 50)
    String email2;

    @Column(name = "cust_email3", length = 50)
    String email3;

    @Column(name = "cust_comment", columnDefinition = "text")
    String comment;

    @Column(name = "cust_is_billing", nullable = false)
    Boolean billing;

    /** Start of activity in the folder ("in billing since"). */
    @Column(name = "cust_ibs")
    LocalDate activeSince;

    /** End of activity in the folder ("active billing stop"). */
    @Column(name = "cust_abs")
    LocalDate activeUntil;

    @Column(name = "cust_mute_date")
    LocalDate muteDate;

    @Column(name = "cust_mute_user", length = 10)
    String muteUser;

    @Override
    public void stamp(LocalDate date, String username) {
        this.muteDate = date;
        this.muteUser = username;
    }
}
