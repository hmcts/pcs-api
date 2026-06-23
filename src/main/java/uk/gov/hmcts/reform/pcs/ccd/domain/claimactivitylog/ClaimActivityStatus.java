package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ClaimActivityStatus implements HasLabel {

    SUCCESS("Success"),
    FAILURE("Failure");

    private final String label;
}
