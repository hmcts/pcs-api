package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

@AllArgsConstructor
@Getter
public enum SecureAntisocialAdditionalGrounds implements PossessionGroundEnum {

    S84A_CONDITION_1("Condition 1 of Section 84A of the Housing Act 1985"),
    S84A_CONDITION_2("Condition 2 of Section 84A of the Housing Act 1985"),
    S84A_CONDITION_3("Condition 3 of Section 84A of the Housing Act 1985"),
    S84A_CONDITION_4("Condition 4 of Section 84A of the Housing Act 1985"),
    S84A_CONDITION_5("Condition 5 of Section 84A of the Housing Act 1985");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }
}
