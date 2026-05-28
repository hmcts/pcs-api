package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;
import static uk.gov.hmcts.reform.pcs.ccd.domain.constants.ReasonForPossessionHintText.REASON_FOR_POSSESSION_HINT_TEXT;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecureOrFlexibleGroundsReasons {

    @CCD(
        label = "Give details about your reasons for claiming possession (Breach of the tenancy (ground 1))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String breachOfTenancyGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Nuisance, annoyance, "
                + "illegal or immoral use of the property (ground 2))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String nuisanceOrImmoralUseGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Domestic violence (ground 2A))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String domesticViolenceGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Offence during a riot (ground 2ZA))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String riotOffenceGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration in the condition "
                + "of the property (ground 3))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String propertyDeteriorationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration of furniture (ground 4))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String furnitureDeteriorationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenancy obtained by "
            + "false statement (ground 5))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String tenancyByFalseStatementGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Premium paid in connection "
                + "with mutual exchange (ground 6))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String premiumMutualExchangeGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Unreasonable conduct in "
            + "tied accommodation (ground 7))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String unreasonableConductGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Refusal to move back to main"
                + " home after works completed (ground 8))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String refusalToMoveBackGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tied accommodation needed for another "
                + "employee (ground 12))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String tiedAccommodationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Adapted accommodation (ground 13))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String adaptedAccommodationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Housing association special circumstances"
                + " accommodation (ground 14))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String housingAssocSpecialGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Special needs accommodation (ground 15))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String specialNeedsAccommodationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Under occupying after "
            + "succession (ground 15A))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String underOccupancySuccessionGround;

    @Deprecated
    @CCD(
        label = "Give details about your reasons for claiming possession (Antisocial behaviour)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antiSocialGround;

    @CCD(
        label = "Give details about your reasons for claiming possession "
                + "(Condition 1 of Section 84A of the Housing Act 1985)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antiSocialCondition1OfS84AGround;

    @CCD(
        label = "Give details about your reasons for claiming possession "
                + "(Condition 2 of Section 84A of the Housing Act 1985)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antiSocialCondition2OfS84AGround;

    @CCD(
        label = "Give details about your reasons for claiming possession "
                + "(Condition 3 of Section 84A of the Housing Act 1985)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antiSocialCondition3OfS84AGround;

    @CCD(
        label = "Give details about your reasons for claiming possession "
                + "(Condition 4 of Section 84A of the Housing Act 1985)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antiSocialCondition4OfS84AGround;

    @CCD(
        label = "Give details about your reasons for claiming possession "
                + "(Condition 5 of Section 84A of the Housing Act 1985)",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antiSocialCondition5OfS84AGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Overcrowding (ground 9))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String overcrowdingGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Landlord’s works (ground 10))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String landlordWorksGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property sold for "
            + "redevelopment (ground 10A))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String propertySoldGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Charitable landlords (ground 11))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String charitableLandlordGround;

}
