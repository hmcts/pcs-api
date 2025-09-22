package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleDiscretionaryGrounds implements HasLabel {

    RENT_ARREARS_OR_BREACH_OF_TENANCY("Rent arrears or breach of the tenancy (ground 1)"),
    NUISANCE_OR_IMMORAL_USE("Nuisance, annoyance, illegal or immoral use of the property (ground 2)"),
    DOMESTIC_VIOLENCE("Domestic violence (ground 2A)"),
    RIOT_OFFENCE("Offence during a riot (ground 2ZA)"),
    PROPERTY_DETERIORATION("Deterioration in the condition of the property (ground 3)"),
    FURNITURE_DETERIORATION("Deterioration of furniture (ground 4)"),
    TENANCY_OBTAINED_BY_FALSE_STATEMENT("Tenancy obtained by false statement (ground 5)"),
    PREMIUM_PAID_MUTUAL_EXCHANGE("Premium paid in connection with mutual exchange (ground 6)"),
    UNREASONABLE_CONDUCT_TIED_ACCOMMODATION("Unreasonable conduct in tied accommodation (ground 7)"),
    REFUSAL_TO_MOVE_BACK("Refusal to move back to main home after works completed (ground 8)");

    private final String label;

}

