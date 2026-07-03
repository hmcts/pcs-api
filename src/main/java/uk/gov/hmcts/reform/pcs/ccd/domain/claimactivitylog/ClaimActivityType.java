package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ClaimActivityType implements HasLabel {

    DOCUMENTS_CREATED("Documents created"),
    DOCUMENT_SENT("Document sent"); // one row per (recipient, document); status SUCCESS = sent, FAILURE = send failed

    private final String label;
}
