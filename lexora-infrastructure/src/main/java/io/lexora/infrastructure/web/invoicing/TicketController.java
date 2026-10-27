package io.lexora.infrastructure.web.invoicing;

import io.lexora.application.ticket.TicketUseCases;
import io.lexora.domain.advance.AdvanceRequestId;
import io.lexora.domain.customer.CustomerId;
import io.lexora.domain.invoice.InvoiceId;
import io.lexora.domain.folder.FolderId;
import io.lexora.domain.ticket.Ticket;
import io.lexora.domain.ticket.TicketId;
import io.lexora.infrastructure.web.Json;
import io.lexora.infrastructure.web.Permissions;
import io.lexora.infrastructure.web.api.TicketsApi;
import io.lexora.infrastructure.web.api.model.TicketDto;
import io.lexora.infrastructure.web.api.model.TicketInputDto;
import io.lexora.infrastructure.web.api.model.TicketNumberInputDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class TicketController implements TicketsApi {

    private final TicketUseCases tickets;

    TicketController(TicketUseCases tickets) {
        this.tickets = tickets;
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_CONTENT)
    public List<TicketDto> listTickets(Long folderId) {
        return Json.map(tickets.listTickets(FolderId.of(folderId)), this::toDto);
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_MODIFY)
    public TicketDto registerTicket(Long folderId, TicketInputDto input) {
        return toDto(tickets.registerTicket(input.getReference(), FolderId.of(folderId),
                input.getCustomerId() == null ? null : CustomerId.of(input.getCustomerId()),
                input.getInvoiceId() == null ? null : InvoiceId.of(input.getInvoiceId()),
                input.getRequestId() == null ? null : AdvanceRequestId.of(input.getRequestId())));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_CONTENT)
    public TicketDto getTicket(Long id) {
        return toDto(tickets.getTicket(TicketId.of(id)));
    }

    @Override
    @PreAuthorize(Permissions.INVOICE_MODIFY)
    public TicketDto renumberTicket(Long id, TicketNumberInputDto input) {
        return toDto(tickets.renumberTicket(TicketId.of(id), input.getNumber()));
    }

    @Override
    @PreAuthorize(Permissions.FOLDER_MODIFY)
    public void deleteTicket(Long id) {
        tickets.deleteTicket(TicketId.of(id));
    }

    private TicketDto toDto(Ticket ticket) {
        return new TicketDto(ticket.id().value()).number(ticket.number()).folderId(Json.id(ticket.folderId()))
                .customerId(Json.id(ticket.customerId())).invoiceId(Json.id(ticket.invoiceId()))
                .requestId(Json.id(ticket.requestId())).cash(ticket.isCash()).used(tickets.isUsed(ticket.id()));
    }
}
