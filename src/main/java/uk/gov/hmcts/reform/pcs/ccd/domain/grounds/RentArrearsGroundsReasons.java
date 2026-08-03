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
public class RentArrearsGroundsReasons {

    // ---------- Mandatory grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Owner occupier (ground 1))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String ownerOccupierReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Repossession by the landlord’s"
            + " mortgage lender (ground 2))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String repossessionByLenderReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Holiday let (ground 3))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String holidayLetReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Student let (ground 4))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String studentLetReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property required for minister of"
            + " religion (ground 5))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String ministerOfReligionReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Property required for "
            + "redevelopment (ground 6))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String redevelopmentReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Death of the tenant (ground 7))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String deathOfTenantReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Antisocial behaviour (ground 7A))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String antisocialBehaviourReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenant does not have a right "
            + "to rent (ground 7B))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String noRightToRentReason;

    // ---------- Discretionary grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Suitable alternative"
            + " accommodation (ground 9))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String suitableAltAccommodationReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Breach of tenancy conditions (ground 12))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String breachOfTenancyConditionsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration in the condition "
            + "of the property (ground 13))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String propertyDeteriorationReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Nuisance, annoyance, illegal or "
            + "immoral use of the property (ground 14))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String nuisanceAnnoyanceReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Domestic violence (ground 14A))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String domesticViolenceReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Offence during a riot (ground 14ZA))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String offenceDuringRiotReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Deterioration of furniture (ground 15))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String furnitureDeteriorationReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Employee of the landlord (ground 16))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String employeeOfLandlordReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Tenancy obtained by false "
            + "statement (ground 17))",
        hint = REASON_FOR_POSSESSION_HINT_TEXT,
        typeOverride = TextArea
    )
    private String tenancyByFalseStatementReason;

    @CCD(
            label = "Give details about your reasons for claiming possession (Other grounds)",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                    + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String otherGroundReason;
}
