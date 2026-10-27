package io.lexora.infrastructure.persistence.customer;

import io.lexora.domain.customer.ActivityPeriod;
import io.lexora.domain.customer.ContactDetails;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.customer.CustomerDetails;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.customer.CustomerRepository;
import io.lexora.domain.customer.PostalAddress;
import io.lexora.domain.folder.FolderId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static io.lexora.infrastructure.persistence.support.Conversions.isTrue;
import static io.lexora.infrastructure.persistence.support.Conversions.toInt;

@Repository
class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpa;

    CustomerRepositoryAdapter(CustomerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpa.findById(toInt(id.value())).map(CustomerRepositoryAdapter::toDomain);
    }

    @Override
    public List<Customer> findByFolder(FolderId folderId) {
        return jpa.findByFolderIdOrderByActiveSinceAscIdAsc(toInt(folderId.value())).stream()
                .map(CustomerRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity e = customer.id() == null
                ? new CustomerEntity()
                : jpa.findById(toInt(customer.id().value())).orElseThrow();
        CustomerDetails d = customer.details();
        e.folderId = toInt(customer.folderId().value());
        e.title = d.title();
        e.firstName = d.firstName();
        e.lastName = d.lastName();
        PostalAddress address = d.address() == null ? new PostalAddress(null, null, null) : d.address();
        e.address = address.street();
        e.zip = address.zip();
        e.city = address.city();
        ContactDetails c = d.contact() == null ? new ContactDetails(null, null, null, null, null, null, null) : d.contact();
        e.privatePhone = c.privatePhone();
        e.professionalPhone = c.professionalPhone();
        e.mobilePhone = c.mobilePhone();
        e.mobilePhone2 = c.mobilePhone2();
        e.email = c.email();
        e.email2 = c.email2();
        e.email3 = c.email3();
        e.comment = d.comment();
        e.billing = d.billing();
        ActivityPeriod activity = d.activity() == null ? ActivityPeriod.ALWAYS : d.activity();
        e.activeSince = activity.since();
        e.activeUntil = activity.until();
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(CustomerId id) {
        jpa.deleteById(toInt(id.value()));
    }

    static Customer toDomain(CustomerEntity e) {
        return Customer.restore(CustomerId.of(e.id), FolderId.of(e.folderId),
                new CustomerDetails(e.title, e.firstName, e.lastName,
                        new PostalAddress(e.address, e.zip, e.city),
                        new ContactDetails(e.privatePhone, e.professionalPhone, e.mobilePhone, e.mobilePhone2,
                                e.email, e.email2, e.email3),
                        e.comment, isTrue(e.billing), new ActivityPeriod(e.activeSince, e.activeUntil)));
    }
}
