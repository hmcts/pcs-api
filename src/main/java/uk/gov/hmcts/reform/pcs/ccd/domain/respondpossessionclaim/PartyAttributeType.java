package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@Getter
@AllArgsConstructor
public enum PartyAttributeType implements HasLabel {

    DEFENDANT_NAME("Defendant name"),
    CORRESPONDENCE_ADDRESS("Correspondence address"),
    TENANCY_TYPE("Tenancy type"),
    TENANCY_START_DATE("Tenancy start date"),
    POSSESSION_NOTICE_RECEIVED("Possession notice received"),
    NOTICE_RECEIVED_DATE("Notice received date"),
    RENT_ARREARS_AMOUNT("Rent arrears amount");

    private final String label;
}
