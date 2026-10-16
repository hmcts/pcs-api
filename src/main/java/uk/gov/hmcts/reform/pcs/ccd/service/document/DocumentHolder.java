package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;

/**
 * An uploaded document together with the type and description to persist it with.
 */
@Builder
@Data
public class DocumentHolder {
    private Document document;
    private DocumentType type;
    private String description;
}
