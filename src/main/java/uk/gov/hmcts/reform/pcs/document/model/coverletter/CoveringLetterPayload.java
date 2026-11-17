package uk.gov.hmcts.reform.pcs.document.model.coverletter;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

/**
 * Docmosis merge-field payload for the covering letter. Field names must match
 * {@code CV-PCS-LET-ENG-Covering-Letter.docx}. The template conditions each optional line on
 * the field itself, so a null value hides that line.
 */
@Data
@Builder
public class CoveringLetterPayload implements FormPayload {

    private String caseReference;
    private String caseName;
    private String letterDate;
    private String signOff;

    private String courtName;
    private String courtAddressLine1;
    private String courtAddressLine2;
    private String courtPostcode;

    private String recipientName;
    private String recipientPersonalName;
    private String recipientOrganisationName;

    private String recipientAddressLine1;
    private String recipientAddressLine2;
    private String recipientPostTown;
    private String recipientCounty;
    private String recipientPostcode;

}
