package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum RiskCategory implements HasLabel {

    VIOLENT_OR_AGGRESSIVE("Violent or aggressive behaviour",
            "How have they been violent or aggressive?"),

    FIREARMS_POSSESSION("History of firearm possession",
            "What is their history of firearm possession?"),

    CRIMINAL_OR_ANTISOCIAL("Criminal or antisocial behaviour",
            "What is their history of criminal or antisocial behaviour?"),

    VERBAL_OR_WRITTEN_THREATS("Verbal or written threats",
            "What kind of verbal or written threats have they made?"),

    PROTEST_GROUP_MEMBER("Member of a group that protests evictions",
            "Which group are they a member of and how have they protested?"),

    AGENCY_VISITS("Police or social services visits to the property",
            "Why did the police or social services visit the property?"),

    AGGRESSIVE_ANIMALS("Aggressive dogs or other animals",
            "What kind of animal do they have?");

    private final String label;
    private final String text;
}


