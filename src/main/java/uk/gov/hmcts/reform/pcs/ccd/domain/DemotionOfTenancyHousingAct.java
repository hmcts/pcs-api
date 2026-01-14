package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DemotionOfTenancyHousingAct implements HasLabel {

    SECTION_82A_2("Section 82A(2) of the Housing Act 1985"),
    SECTION_6A_2("Section 6A(2) of the Housing Act 1988");

    private final String label;
}
