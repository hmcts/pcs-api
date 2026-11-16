package uk.gov.hmcts.reform.pcs.document.model.hearingnotice;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

/**
 * Docmosis merge-field payload for the notice of hearing, one per recipient. Field names must match
 * {@code CV-PCS-HNO-ENG-Hearing-Notice.docx}.
 */
@Data
@Builder
public class HearingNoticePayload implements FormPayload {

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
    private String courtName;
    private String caseReference;
    private String claimantName;
    private String defendantName;
    private String hearingTime;
    private String hearingDate;
    private String url;

}
