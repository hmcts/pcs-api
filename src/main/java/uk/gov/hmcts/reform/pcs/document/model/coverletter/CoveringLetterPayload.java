package uk.gov.hmcts.reform.pcs.document.model.coverletter;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

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
    private boolean hasCourtAddressLine2;

    private String recipientName;
    private String recipientPersonalName;
    private String recipientOrganisationName;
    private boolean hasRecipientPersonalName;
    private boolean hasRecipientOrganisationName;

    private String recipientAddressLine1;
    private String recipientAddressLine2;
    private String recipientPostTown;
    private String recipientCounty;
    private String recipientPostcode;
    private boolean hasRecipientAddressLine2;
    private boolean hasRecipientCounty;

}
