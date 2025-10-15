package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecureOrFlexibleGroundsReasons {

    @CCD(
        label = "Give details about your reasons for claiming possession (Breach of the tenancy (ground 1))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String breachOfTenancyGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Nuisance, annoyance, "
                + "illegal or immoral use of the property (ground 2))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String nuisanceOrImmoralUseGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Domestic violence (ground 2A))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String domesticViolenceGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Offence during a riot (ground 2ZA))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String riotOffenceGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration in the condition "
                + "of the property (ground 3))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String propertyDeteriorationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration of furniture (ground 4))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String furnitureDeteriorationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenancy obtained by "
            + "false statement (ground 5))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String tenancyByFalseStatementGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Premium paid in connection "
                + "with mutual exchange (ground 6))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String premiumMutualExchangeGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Unreasonable conduct in "
            + "tied accommodation (ground 7))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String unreasonableConductGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Refusal to move back to main"
                + " home after works completed (ground 8))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String refusalToMoveBackGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tied accommodation needed for another "
                + "employee (ground 12))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String tiedAccommodationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Adapted accommodation (ground 13))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String adaptedAccommodationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Housing association special circumstances"
                + " accommodation (ground 14))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String housingAssocSpecialGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Special needs accommodation (ground 15))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String specialNeedsAccommodationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Under occupying after "
            + "succession (ground 15A))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String underOccupancySuccessionGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Antisocial behaviour)",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String antiSocialGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Overcrowding (ground 9))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String overcrowdingGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Landlord's works (ground 10))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String landlordWorksGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property sold for "
            + "redevelopment (ground 10A))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String propertySoldGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Charitable landlords (ground 11))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea
    )
    private String charitableLandlordGround;

}
