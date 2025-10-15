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
public class RentArrearsGroundReasons {

    // ---------- Mandatory grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Owner occupier (ground 1))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String ownerOccupierGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Repossession by the landlord's"
            + " mortgage lender (ground 2))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String repossessionByLenderGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Holiday let (ground 3))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String holidayLetGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Student let (ground 4))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String studentLetGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property required for minister of"
            + " religion (ground 5))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String ministerOfReligionGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property required for "
            + "redevelopment (ground 6))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String redevelopmentGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Death of the tenant (ground 7))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String deathOfTenantGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Antisocial behaviour (ground 7A))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String antisocialBehaviourGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenant does not have a right "
            + "to rent (ground 7B))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String noRightToRentGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Serious rent arrears (ground 8))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String seriousRentArrearsGround;


    // ---------- Discretionary grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Suitable alternative"
            + " accommodation (ground 9))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String suitableAltAccommodationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Rent arrears (ground 10))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String rentArrearsGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Persistent delay in paying "
            + "rent (ground 11))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String persistentDelayRentGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Breach of tenancy conditions (ground 12))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String breachOfTenancyConditionsGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration in the condition "
            + "of the property (ground 13))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String propertyDeteriorationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Nuisance, annoyance, illegal or "
            + "immoral use of the property (ground 14))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String nuisanceAnnoyanceGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Domestic violence (ground 14A))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String domesticViolenceGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Offence during a riot (ground 14ZA))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String offenceDuringRiotGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration of furniture (ground 15))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String furnitureDeteriorationGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Employee of the landlord (ground 16))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String employeeOfLandlordGround;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenancy obtained by false "
            + "statement (ground 17))",
        hint = "You'll be able to upload documents to support or further explain your reasons later on",
        typeOverride = TextArea,
        max = 500
    )
    private String tenancyByFalseStatementGround;
}
