package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Declared in the order a batch print prints them.
 */
@AllArgsConstructor
@Getter
public enum BatchPrintDocumentKind {

    WARRANT("Warrant", Set.of(DocumentType.WARRANT_OF_POSSESSION, DocumentType.WARRANT_OF_RESTITUTION)),
    RISK_ASSESSMENT("Risk assessment", Set.of(DocumentType.RISK_ASSESSMENT)),
    NOTICE_OF_EVICTION("Notice of eviction", Set.of(DocumentType.NOTICE_OF_EVICTION));

    private final String label;
    private final Set<DocumentType> documentTypes;

    public static Optional<BatchPrintDocumentKind> of(DocumentType documentType) {
        return Arrays.stream(values())
            .filter(kind -> kind.documentTypes.contains(documentType))
            .findFirst();
    }
}
