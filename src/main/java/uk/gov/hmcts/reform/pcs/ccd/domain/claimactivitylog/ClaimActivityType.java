package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ClaimActivityType implements HasLabel {

    DOCUMENTS_CREATED("Documents created"),
    PACK_SENT("Pack sent"),     // one row per (recipient, pack) dispatch; PackDetails in the details column
    PACK_FAILED("Pack failed"); // dispatch attempt failed; PackDetails carries failureReason + terminal

    private final String label;
}
