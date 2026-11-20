package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The sections of the citizen claim journey that end in a check your answers page. Each code is the
 * pcs-frontend step name of that page.
 */
@AllArgsConstructor
@Getter
public enum CitizenClaimSection implements HasLabel {

    CLAIMANT("claimant-cya", "Claimant details"),
    DEFENDANT("defendant-cya", "Defendant details"),
    TENANCY("tenancy-cya", "Tenancy and grounds"),
    NOTICE("notice-cya", "Notice"),
    ARREARS("arrears-cya", "Rent arrears"),
    SITUATION("situation-cya", "Circumstances and other information");

    @JsonValue
    private final String code;
    private final String label;
}
