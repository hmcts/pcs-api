package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum IntroductoryDemotedOrOtherGrounds implements HasLabel {

    RENT_ARREARS("Rent arrears"),
    ANTI_SOCIAL("Antisocial behaviour"),
    BREACH_OF_THE_TENANCY("Breach of the tenancy"),
    ABSOLUTE_GROUNDS("Absolute grounds"),
    OTHER("Other");

    private final String label;
}
