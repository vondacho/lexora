package io.lexora.domain.user;

import java.util.List;

/** Every grant known by the application (legacy {@code grants.php}). */
public final class GrantCatalog {

    public static final String ADMIN = "admin";
    public static final String BOOKING = "booking";
    public static final String INVOICING = "invoicing";

    public static final List<Grant> ALL = List.of(
            Grant.of(ADMIN, "users"),

            Grant.of(BOOKING, "payments"),
            Grant.of(BOOKING, "payments_list"),
            Grant.of(BOOKING, "payment_display"),
            Grant.of(BOOKING, "payment_create"),
            Grant.of(BOOKING, "payment_modify"),
            Grant.of(BOOKING, "orders"),
            Grant.of(BOOKING, "orders_list"),
            Grant.of(BOOKING, "order_create"),
            Grant.of(BOOKING, "order_modify"),
            Grant.of(BOOKING, "tax_summary"),
            Grant.of(BOOKING, "vat_payment"),
            Grant.of(BOOKING, "overview"),
            Grant.of(BOOKING, "statistics_search"),
            Grant.of(BOOKING, "parameters_modify"),

            Grant.of(INVOICING, "folder_search"),
            Grant.of(INVOICING, "folder_create"),
            Grant.of(INVOICING, "folder_display"),
            Grant.of(INVOICING, "folder_modify"),
            Grant.of(INVOICING, "folder_status_modify"),
            Grant.of(INVOICING, "folder_content"),
            Grant.of(INVOICING, "folder_requests"),
            Grant.of(INVOICING, "folder_invoices"),
            Grant.of(INVOICING, "folder_honoraries"),
            Grant.of(INVOICING, "customer_create"),
            Grant.of(INVOICING, "customer_modify"),
            Grant.of(INVOICING, "customer_display"),
            Grant.of(INVOICING, "customer_folder"),
            Grant.of(INVOICING, "customer_honoraries"),
            Grant.of(INVOICING, "customer_invoices"),
            Grant.of(INVOICING, "customer_advances"),
            Grant.of(INVOICING, "customer_requests"),
            Grant.of(INVOICING, "honorary_create"),
            Grant.of(INVOICING, "honorary_modify"),
            Grant.of(INVOICING, "invoice_create"),
            Grant.of(INVOICING, "invoice_modify"),
            Grant.of(INVOICING, "invoice_display"),
            Grant.of(INVOICING, "request_create"),
            Grant.of(INVOICING, "request_modify"),
            Grant.of(INVOICING, "request_display"),
            Grant.of(INVOICING, "debtor_search"),
            Grant.of(INVOICING, "debtor_receipt"),
            Grant.of(INVOICING, "advance_receipt"),
            Grant.of(INVOICING, "debtors_payed"),
            Grant.of(INVOICING, "debtors_open"),
            Grant.of(INVOICING, "parameters_modify"),
            Grant.of(INVOICING, "wordings_modify"),
            Grant.of(INVOICING, "models"));

    private GrantCatalog() {
    }

    public static boolean contains(Grant grant) {
        return ALL.contains(grant);
    }
}
