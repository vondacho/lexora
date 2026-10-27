package ch.conciliations.mabecoju.infrastructure.web;

/**
 * {@code @PreAuthorize} expressions mirroring the page grants of the legacy application ({@code module:page}).
 */
public final class Permissions {

    public static final String AUTHENTICATED = "isAuthenticated()";

    public static final String ADMIN_USERS = "hasAuthority('admin:users')";

    public static final String FOLDER_SEARCH = "hasAuthority('invoicing:folder_search')";
    public static final String FOLDER_CREATE = "hasAuthority('invoicing:folder_create')";
    public static final String FOLDER_DISPLAY = "hasAuthority('invoicing:folder_display')";
    public static final String FOLDER_MODIFY = "hasAuthority('invoicing:folder_modify')";
    public static final String FOLDER_STATUS_MODIFY = "hasAuthority('invoicing:folder_status_modify')";
    public static final String FOLDER_CONTENT = "hasAuthority('invoicing:folder_content')";
    public static final String FOLDER_REQUESTS = "hasAuthority('invoicing:folder_requests')";
    public static final String FOLDER_INVOICES = "hasAuthority('invoicing:folder_invoices')";
    public static final String FOLDER_HONORARIES = "hasAuthority('invoicing:folder_honoraries')";
    public static final String CUSTOMER_CREATE = "hasAuthority('invoicing:customer_create')";
    public static final String CUSTOMER_MODIFY = "hasAuthority('invoicing:customer_modify')";
    public static final String CUSTOMER_DISPLAY = "hasAuthority('invoicing:customer_display')";
    public static final String HONORARY_CREATE = "hasAuthority('invoicing:honorary_create')";
    public static final String HONORARY_MODIFY = "hasAuthority('invoicing:honorary_modify')";
    public static final String INVOICE_CREATE = "hasAuthority('invoicing:invoice_create')";
    public static final String INVOICE_MODIFY = "hasAuthority('invoicing:invoice_modify')";
    public static final String INVOICE_DISPLAY = "hasAuthority('invoicing:invoice_display')";
    public static final String REQUEST_CREATE = "hasAuthority('invoicing:request_create')";
    public static final String REQUEST_MODIFY = "hasAuthority('invoicing:request_modify')";
    public static final String REQUEST_DISPLAY = "hasAuthority('invoicing:request_display')";
    public static final String DEBTOR_SEARCH = "hasAuthority('invoicing:debtor_search')";
    public static final String DEBTOR_RECEIPT = "hasAuthority('invoicing:debtor_receipt')";
    public static final String ADVANCE_RECEIPT = "hasAuthority('invoicing:advance_receipt')";
    public static final String DEBTORS_PAYED = "hasAuthority('invoicing:debtors_payed')";
    public static final String DEBTORS_OPEN = "hasAuthority('invoicing:debtors_open')";
    public static final String INVOICING_PARAMETERS = "hasAuthority('invoicing:parameters_modify')";
    public static final String WORDINGS_MODIFY = "hasAuthority('invoicing:wordings_modify')";
    public static final String MODELS = "hasAuthority('invoicing:models')";

    public static final String BOOKING_PAYMENTS_LIST = "hasAuthority('booking:payments_list')";
    public static final String BOOKING_PAYMENT_DISPLAY = "hasAuthority('booking:payment_display')";
    public static final String BOOKING_PAYMENT_CREATE = "hasAuthority('booking:payment_create')";
    public static final String BOOKING_PAYMENT_MODIFY = "hasAuthority('booking:payment_modify')";
    public static final String BOOKING_ORDERS = "hasAuthority('booking:orders')";
    public static final String BOOKING_ORDERS_LIST = "hasAuthority('booking:orders_list')";
    public static final String BOOKING_ORDER_CREATE = "hasAuthority('booking:order_create')";
    public static final String BOOKING_ORDER_MODIFY = "hasAuthority('booking:order_modify')";
    public static final String BOOKING_TAX_SUMMARY = "hasAuthority('booking:tax_summary')";
    public static final String BOOKING_VAT_PAYMENT = "hasAuthority('booking:vat_payment')";
    public static final String BOOKING_OVERVIEW = "hasAuthority('booking:overview')";
    public static final String BOOKING_STATISTICS = "hasAuthority('booking:statistics_search')";
    public static final String BOOKING_PARAMETERS = "hasAuthority('booking:parameters_modify')";

    private Permissions() {
    }
}
