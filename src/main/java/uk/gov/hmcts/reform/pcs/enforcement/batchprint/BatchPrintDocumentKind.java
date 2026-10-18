package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * The documents a bailiff prints for each case (HDPI-8012), in the order they are printed.
 */
@AllArgsConstructor
@Getter
public enum BatchPrintDocumentKind {

    WARRANT("Warrant", Set.of(DocumentType.WARRANT)),
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
