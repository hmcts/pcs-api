package uk.gov.hmcts.reform.pcs.document.model.coversheet;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

/**
 * Docmosis merge-field payload for the bulk-print coversheet. One generic template serves every pack;
 * field names must match {@code CV-PCS-LET-ENG-Coversheet.docx}.
 */
@Data
@Builder
public class CoversheetPayload implements FormPayload {

    private String caseReference;
    private String recipientName;
    private String recipientAddressLine1;
    private String recipientAddressLine2;
    private String recipientAddressLine3;
    private String recipientPostTown;
    private String recipientCounty;
    private String recipientPostcode;
    private boolean hasAddressLine2;
    private boolean hasAddressLine3;
    private boolean hasCounty;

}
