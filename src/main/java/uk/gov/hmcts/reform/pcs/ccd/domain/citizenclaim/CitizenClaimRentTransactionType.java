package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CitizenClaimRentTransactionType implements HasLabel {

    RENT_CHARGE("Rent charge"),
    TENANT_PAYMENT("Payment from tenant");

    private final String label;
}
