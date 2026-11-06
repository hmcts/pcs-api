package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

/**
 * Assured tenancy grounds offered to self-represented private landlords in England, including the
 * grounds added by the Renters' Rights Act. Kept apart from the social-landlord ground enums.
 */
@AllArgsConstructor
@Getter
public enum PrivateLandlordAssuredGround implements PossessionGroundEnum {

    GROUND_1("Occupation by landlord or family (Ground 1)", true, false),
    GROUND_1A("Sale of dwelling house (Ground 1A)", true, false),
    GROUND_1B("Sale of dwelling house that is part of Rent to Buy (Ground 1B)", true, false),
    GROUND_2("Sale by mortgagee (Ground 2)", true, false),
    GROUND_2ZA("Possession when superior lease ends (Ground 2ZA)", true, false),
    GROUND_2ZB("Possession when fixed term superior lease ends (Ground 2ZB)", true, false),
    GROUND_2ZC("Possession by superior landlord (Ground 2ZC)", true, false),
    GROUND_2ZD("Possession by superior landlord (fixed term) (Ground 2ZD)", true, false),
    GROUND_4("Student accommodation (Ground 4)", true, false),
    GROUND_4A("Properties rented to students for occupation by new students (Ground 4A)", true, false),
    GROUND_5("Ministers of religion (Ground 5)", true, false),
    GROUND_5A("Occupation by agricultural worker (Ground 5A)", true, false),
    GROUND_5B("Occupation by person who meets employment requirements (Ground 5B)", true, false),
    GROUND_5C("End of employment by the landlord (Ground 5C)", true, false),
    GROUND_5D("End of employment requirements (Ground 5D)", true, false),
    GROUND_5E("Occupation as supported accommodation (Ground 5E)", true, false),
    GROUND_5F("Dwelling-house occupied as supported accommodation (Ground 5F)", true, false),
    GROUND_5G("Tenancy granted for homelessness duty (Ground 5G)", true, false),
    GROUND_5H("Occupation as ‘stepping stone accommodation’ (Ground 5H)", true, false),
    GROUND_6("Redevelopment (Ground 6)", true, false),
    GROUND_6A("Decant accommodation (Ground 6A)", true, false),
    GROUND_6B("Compliance with enforcement action (Ground 6B)", true, false),
    GROUND_7("Death of tenant (Ground 7)", true, false),
    GROUND_7A("Severe antisocial or criminal behaviour (Ground 7A)", true, false),
    GROUND_7B("No right to rent (Ground 7B)", true, false),
    GROUND_8("Rent arrears (Ground 8)", true, true),
    OTHER("Other grounds", true, false),
    GROUND_9("Suitable alternative accommodation (Ground 9)", false, false),
    GROUND_10("Any rent arrears (Ground 10)", false, true),
    GROUND_11("Persistent arrears (Ground 11)", false, true),
    GROUND_12("Breach of tenancy (Ground 12)", false, false),
    GROUND_13("Deterioration of property (Ground 13)", false, false),
    GROUND_14("Antisocial behaviour (Ground 14)", false, false),
    GROUND_14A("Domestic abuse (Ground 14A)", false, false),
    GROUND_14ZA("Rioting (Ground 14ZA)", false, false),
    GROUND_15("Deterioration of furniture (Ground 15)", false, false),
    GROUND_17("False statement (Ground 17)", false, false),
    GROUND_18("Supported accommodation (Ground 18)", false, false);

    private final String label;
    private final boolean mandatory;
    private final boolean rentArrears;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
