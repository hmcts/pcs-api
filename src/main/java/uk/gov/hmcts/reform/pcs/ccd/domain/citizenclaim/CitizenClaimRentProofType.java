package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CitizenClaimRentProofType implements HasLabel {

    UPLOAD_STATEMENT("Upload a rent statement"),
    LEDGER("Manually enter a history of payments"),
    TABLE("Create a rent statement");

    private final String label;
}
