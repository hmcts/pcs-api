package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

/**
 * Discretionary grounds for assured tenancy possession claims.
 */
@AllArgsConstructor
@Getter
public enum AssuredDiscretionaryGround implements PossessionGroundEnum {

    ALTERNATIVE_ACCOMMODATION_GROUND9("Suitable alternative accommodation (ground 9)"),
    RENT_ARREARS_GROUND10("Rent arrears (ground 10)"),
    PERSISTENT_DELAY_GROUND11("Persistent delay in paying rent (ground 11)"),
    BREACH_TENANCY_GROUND12("Breach of tenancy conditions (ground 12)"),
    DETERIORATION_PROPERTY_GROUND13("Deterioration in the condition of the property (ground 13)"),
    NUISANCE_ANNOYANCE_GROUND14("Nuisance, annoyance, illegal or immoral use of the property (ground 14)"),
    DOMESTIC_VIOLENCE_GROUND14A("Domestic violence (ground 14A)"),
    OFFENCE_RIOT_GROUND14ZA("Offence during a riot (ground 14ZA)"),
    DETERIORATION_FURNITURE_GROUND15("Deterioration of furniture (ground 15)"),
    EMPLOYEE_LANDLORD_GROUND16("Employee of the landlord (ground 16)"),
    FALSE_STATEMENT_GROUND17("Tenancy obtained by false statement (ground 17)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
