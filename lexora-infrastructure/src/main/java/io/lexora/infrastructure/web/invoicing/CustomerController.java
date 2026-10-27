package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.customer.CustomerUseCases;
import io.lexora.domain.customer.Customer;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.folder.FolderId;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.CustomersApi;
import io.lexora.infrastructure.web.api.model.CustomerDto;
import io.lexora.infrastructure.web.api.model.CustomerInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
class CustomerController implements CustomersApi {

    private final CustomerUseCases customers;

    CustomerController(CustomerUseCases customers) {
        this.customers = customers;
    }

    @Override
    @PreAuthorize(Permissions.CUSTOMER_DISPLAY)
    public List<CustomerDto> listCustomers(Long folderId, LocalDate activeAt) {
        List<Customer> found = activeAt == null
                ? customers.listCustomers(FolderId.of(folderId))
                : customers.listActiveCustomers(FolderId.of(folderId), activeAt);
        return Json.map(found, InvoicingMapper::toDto);
    }

    @Override
    @PreAuthorize(Permissions.CUSTOMER_CREATE)
    public CustomerDto addCustomer(Long folderId, CustomerInputDto input) {
        return InvoicingMapper.toDto(customers.addCustomer(FolderId.of(folderId), InvoicingMapper.toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.CUSTOMER_DISPLAY)
    public CustomerDto getCustomer(Long id) {
        return InvoicingMapper.toDto(customers.getCustomer(CustomerId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.CUSTOMER_MODIFY)
    public CustomerDto updateCustomer(Long id, CustomerInputDto input) {
        return InvoicingMapper.toDto(customers.updateCustomer(CustomerId.of(id), InvoicingMapper.toDetails(input)));
    }

    @Override
    @PreAuthorize(Permissions.CUSTOMER_MODIFY)
    public void removeCustomer(Long id) {
        customers.removeCustomer(CustomerId.of(id));
    }
}
