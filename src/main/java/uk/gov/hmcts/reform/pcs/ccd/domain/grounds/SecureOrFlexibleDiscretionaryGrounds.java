package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleDiscretionaryGrounds implements PossessionGroundEnum {

    RENT_ARREARS_OR_BREACH_OF_TENANCY_GROUND1("Rent arrears or breach of the tenancy (ground 1)"),
    NUISANCE_OR_IMMORAL_USE_GROUND2("Nuisance, annoyance, illegal or immoral use of the property (ground 2)"),
    DOMESTIC_VIOLENCE_GROUND2A("Domestic violence (ground 2A)"),
    RIOT_OFFENCE_GROUND2ZA("Offence during a riot (ground 2ZA)"),
    PROPERTY_DETERIORATION_GROUND3("Deterioration in the condition of the property (ground 3)"),
    FURNITURE_DETERIORATION_GROUND4("Deterioration of furniture (ground 4)"),
    TENANCY_FALSE_STATEMENT_GROUND5("Tenancy obtained by false statement (ground 5)"),
    PREMIUM_PAID_GROUND6("Premium paid in connection with mutual exchange (ground 6)"),
    UNREASONABLE_CONDUCT_GROUND7("Unreasonable conduct in tied accommodation (ground 7)"),
    REFUSAL_TO_MOVE_BACK_GROUND8("Refusal to move back to main home after works completed (ground 8)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}

