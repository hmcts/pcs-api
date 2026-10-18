package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import java.util.List;

/**
 * A case that is printed, with its documents in print order and the kinds it has no document for yet.
 */
public record PrintableCase(String caseReference,
                            String warrantNumber,
                            List<PrintableDocument> documents,
                            List<BatchPrintDocumentKind> missingDocuments) {
}
