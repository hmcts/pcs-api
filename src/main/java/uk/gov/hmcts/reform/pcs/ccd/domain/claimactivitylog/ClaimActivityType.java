package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ClaimActivityType implements HasLabel {

    DOCUMENTS_CREATED("Documents created"),
    // CLAIMANT_PACK_SENT/DEFENDANT_PACK_SENT exist in the DB enum (V111 on master) but are no longer written;
    // all sends are now recorded per (recipient, document) as DOCUMENT_SENT.
    CLAIMANT_PACK_SENT("Claimant pack sent"),
    DEFENDANT_PACK_SENT("Defendant pack sent"),
    DOCUMENT_SENT("Document sent"); // one row per (recipient, document); status SUCCESS = sent, FAILURE = send failed

    private final String label;
}
