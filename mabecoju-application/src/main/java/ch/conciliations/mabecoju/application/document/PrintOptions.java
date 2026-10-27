package ch.conciliations.mabecoju.application.document;

import ch.conciliations.mabecoju.domain.template.DocumentModelId;

/**
 * Choices made when printing a document.
 *
 * @param modelId        letter template to use
 * @param content        letter text edited for this print, or null to use the template's text
 * @param attachments    number of attachments mentioned in the footer of the first page (0 for none)
 * @param mergeAddresses one letter to all the customers together instead of one copy per customer
 */
public record PrintOptions(DocumentModelId modelId, String content, int attachments, boolean mergeAddresses) {
}
