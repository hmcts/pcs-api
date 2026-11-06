package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;

import static uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_DISCRETIONARY;
import static uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_MANDATORY;
import static uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory.PRIVATE_LANDLORD_ASSURED_OTHER;

/**
 * Assured tenancy grounds offered to self-represented private landlords in England, including the
 * grounds added by the Renters' Rights Act. Kept apart from the social-landlord ground enums.
 */
@AllArgsConstructor
@Getter
public enum PrivateLandlordAssuredGround implements PossessionGroundEnum {

    GROUND_1("Occupation by landlord or family (Ground 1)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_1A("Sale of dwelling house (Ground 1A)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_1B("Sale of dwelling house that is part of Rent to Buy (Ground 1B)",
              PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_2("Sale by mortgagee (Ground 2)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_2ZA("Possession when superior lease ends (Ground 2ZA)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_2ZB("Possession when fixed term superior lease ends (Ground 2ZB)",
               PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_2ZC("Possession by superior landlord (Ground 2ZC)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_2ZD("Possession by superior landlord (fixed term) (Ground 2ZD)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_4("Student accommodation (Ground 4)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_4A("Properties rented to students for occupation by new students (Ground 4A)",
              PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5("Ministers of religion (Ground 5)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5A("Occupation by agricultural worker (Ground 5A)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5B("Occupation by person who meets employment requirements (Ground 5B)",
              PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5C("End of employment by the landlord (Ground 5C)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5D("End of employment requirements (Ground 5D)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5E("Occupation as supported accommodation (Ground 5E)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5F("Dwelling-house occupied as supported accommodation (Ground 5F)",
              PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5G("Tenancy granted for homelessness duty (Ground 5G)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_5H("Occupation as ‘stepping stone accommodation’ (Ground 5H)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_6("Redevelopment (Ground 6)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_6A("Decant accommodation (Ground 6A)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_6B("Compliance with enforcement action (Ground 6B)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_7("Death of tenant (Ground 7)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_7A("Severe antisocial or criminal behaviour (Ground 7A)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_7B("No right to rent (Ground 7B)", PRIVATE_LANDLORD_ASSURED_MANDATORY, false),
    GROUND_8("Rent arrears (Ground 8)", PRIVATE_LANDLORD_ASSURED_MANDATORY, true),
    GROUND_9("Suitable alternative accommodation (Ground 9)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_10("Any rent arrears (Ground 10)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, true),
    GROUND_11("Persistent arrears (Ground 11)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, true),
    GROUND_12("Breach of tenancy (Ground 12)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_13("Deterioration of property (Ground 13)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_14("Antisocial behaviour (Ground 14)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_14A("Domestic abuse (Ground 14A)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_14ZA("Rioting (Ground 14ZA)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_15("Deterioration of furniture (Ground 15)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_17("False statement (Ground 17)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    GROUND_18("Supported accommodation (Ground 18)", PRIVATE_LANDLORD_ASSURED_DISCRETIONARY, false),
    OTHER("Other grounds", PRIVATE_LANDLORD_ASSURED_OTHER, false);

    private final String label;
    private final ClaimGroundCategory category;
    private final boolean rentArrears;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
