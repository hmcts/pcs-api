package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import java.util.List;

public record PrintableCase(String caseReference,
                            String warrantNumber,
                            List<PrintableDocument> documents,
                            List<BatchPrintDocumentKind> missingDocuments) {
}
