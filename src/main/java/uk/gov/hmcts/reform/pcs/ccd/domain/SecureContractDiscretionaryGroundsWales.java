package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum SecureContractDiscretionaryGroundsWales implements HasLabel {

    RENT_ARREARS("Rent arrears (breach of contract)(section 157)"),
    ANTISOCIAL_BEHAVIOUR("Anti-social behaviour (breach of contract)(section 157)"),
    OTHER_BREACH_OF_CONTRACT("Other breach of contract (section 157)"),
    ESTATE_MANAGEMENT_GROUNDS("Estate management grounds (section 160)"),;

    private final String label;

}

