package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Set;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@Getter
public enum ClaimantType implements HasLabel {

    PRIVATE_LANDLORD("Private landlord", Set.of(ENGLAND, WALES)),
    PROVIDER_OF_SOCIAL_HOUSING("Registered provider of social housing", Set.of(ENGLAND)),
    COMMUNITY_LANDLORD("Registered community landlord", Set.of(WALES)),
    MORTGAGE_LENDER("Mortgage lender", Set.of(ENGLAND, WALES)),
    OTHER("Other", Set.of(ENGLAND, WALES));

    private final String label;
    private final Set<LegislativeCountry> legislativeCountries;

    ClaimantType(String label, Set<LegislativeCountry> legislativeCountries) {
        this.label = label;
        this.legislativeCountries = requireNonNull(legislativeCountries, "legistive countries must not be null");
    }

    public boolean isApplicableFor(LegislativeCountry legislativeCountry) {
        return legislativeCountries.contains(legislativeCountry);
    }

}
