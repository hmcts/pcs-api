package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DiscretionaryGroundWales implements HasLabel {
    RENT_ARREARS_S157("Rent arrears (breach of contract) (section 157)"),
    ANTISOCIAL_BEHAVIOUR_S157("Antisocial behaviour (breach of contract) (section 157)"),
    OTHER_BREACH_S157("Other breach of contract (section 157)"),
    ESTATE_MANAGEMENT_GROUNDS_S160("Estate management grounds (section 160)");

    private final String label;
}
