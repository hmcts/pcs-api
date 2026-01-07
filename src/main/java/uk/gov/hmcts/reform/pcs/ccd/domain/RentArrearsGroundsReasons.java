package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentArrearsGroundsReasons {

    // ---------- Mandatory grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Owner occupier (ground 1))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String ownerOccupierReason;

    @CCD
    private YesOrNo showOwnerOccupierReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Repossession by the landlord’s"
            + " mortgage lender (ground 2))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String repossessionByLenderReason;

    @CCD
    private YesOrNo showRepossessionByLenderReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Holiday let (ground 3))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String holidayLetReason;

    @CCD
    private YesOrNo showHolidayLetReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Student let (ground 4))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String studentLetReason;

    @CCD
    private YesOrNo showStudentLetReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property required for minister of"
            + " religion (ground 5))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String ministerOfReligionReason;

    @CCD
    private YesOrNo showMinisterOfReligionReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property required for "
            + "redevelopment (ground 6))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String redevelopmentReason;

    @CCD
    private YesOrNo showRedevelopmentReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Death of the tenant (ground 7))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String deathOfTenantReason;

    @CCD
    private YesOrNo showDeathOfTenantReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Antisocial behaviour (ground 7A))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String antisocialBehaviourReason;

    @CCD
    private YesOrNo showAntisocialBehaviourReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenant does not have a right "
            + "to rent (ground 7B))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String noRightToRentReason;

    @CCD
    private YesOrNo showNoRightToRentReason;

    // ---------- Discretionary grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Suitable alternative"
            + " accommodation (ground 9))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String suitableAltAccommodationReason;

    @CCD
    private YesOrNo showSuitableAltAccommodationReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Breach of tenancy conditions (ground 12))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String breachOfTenancyConditionsReason;

    @CCD
    private YesOrNo showBreachOfTenancyConditionsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration in the condition "
            + "of the property (ground 13))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String propertyDeteriorationReason;

    @CCD
    private YesOrNo showPropertyDeteriorationReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Nuisance, annoyance, illegal or "
            + "immoral use of the property (ground 14))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String nuisanceAnnoyanceReason;

    @CCD
    private YesOrNo showNuisanceAnnoyanceReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Domestic violence (ground 14A))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String domesticViolenceReason;

    @CCD
    private YesOrNo showDomesticViolenceReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Offence during a riot (ground 14ZA))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String offenceDuringRiotReason;

    @CCD
    private YesOrNo showOffenceDuringRiotReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration of furniture (ground 15))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String furnitureDeteriorationReason;

    @CCD
    private YesOrNo showFurnitureDeteriorationReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Employee of the landlord (ground 16))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String employeeOfLandlordReason;

    @CCD
    private YesOrNo showEmployeeOfLandlordReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenancy obtained by false "
            + "statement (ground 17))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String tenancyByFalseStatementReason;

    @CCD
    private YesOrNo showTenancyByFalseStatementReason;
}
