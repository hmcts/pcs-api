package uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ClaimActivityType implements HasLabel {

    DOCUMENTS_CREATED("Documents created"),
    CLAIMANT_PACK_SENT("Claimant pack sent"),
    DEFENDANT_PACK_SENT("Defendant pack sent");

    private final String label;
}
