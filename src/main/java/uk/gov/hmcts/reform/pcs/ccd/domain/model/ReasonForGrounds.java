package uk.gov.hmcts.reform.pcs.ccd.domain.model;

import lombok.*;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.Label;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasonForGrounds {

    // Ground 1
    @CCD(label = """
            <h2>Owner occupier (ground 1)</h2>
            """,
        typeOverride = FieldType.Label)
    private String ownerOccupierLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String ownerOccupierGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String ownerOccupierTextArea;

    // Ground 2
    @CCD(label = """
            <h2>Repossession by the landlord's mortgage lender (ground 2)</h2>
            """,
        typeOverride = FieldType.Label)
    private String repossessionByLenderLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String repossessionByLenderGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String repossessionByLenderTextArea;

    // Ground 3
    @CCD(label = """
            <h2>Holiday let (ground 3)</h2>
            """,
        typeOverride = FieldType.Label)
    private String holidayLetLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String holidayLetGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String holidayLetTextArea;

    // Ground 4
    @CCD(label = """
            <h2>Student let (ground 4)</h2>
            """,
        typeOverride = FieldType.Label)
    private String studentLetLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String studentLetGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String studentLetTextArea;

    // Ground 5
    @CCD(label = """
            <h2>Property required for minister of religion (ground 5)</h2>
            """,
        typeOverride = FieldType.Label)
    private String ministerOfReligionLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String ministerOfReligionGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String ministerOfReligionTextArea;

    // Ground 6
    @CCD(label = """
            <h2>Property required for redevelopment (ground 6)</h2>
            """,
        typeOverride = FieldType.Label)
    private String redevelopmentLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String redevelopmentGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String redevelopmentTextArea;

    // Ground 7
    @CCD(label = """
            <h2>Death of the tenant (ground 7)</h2>
            """,
        typeOverride = FieldType.Label)
    private String deathOfTenantLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String deathOfTenantGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String deathOfTenantTextArea;

    // Ground 7A
    @CCD(label = """
            <h2>Antisocial behaviour (ground 7A)</h2>
            """,
        typeOverride = FieldType.Label)
    private String antisocialBehaviourLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String antisocialBehaviourGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String antisocialBehaviourTextArea;

    // Ground 7B
    @CCD(label = """
            <h2>Tenant does not have a right to rent (ground 7B)</h2>
            """,
        typeOverride = FieldType.Label)
    private String noRightToRentLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String noRightToRentGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String noRightToRentTextArea;

    // Ground 8
    @CCD(label = """
            <h2>Serious rent arrears (ground 8)</h2>
            """,
        typeOverride = FieldType.Label)
    private String seriousRentArrearsLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String seriousRentArrearsGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String seriousRentArrearsTextArea;

    // Ground 9
    @CCD(label = """
            <h2>Suitable alternative accommodation (ground 9)</h2>
            """,
        typeOverride = FieldType.Label)
    private String suitableAlternativeAccommodationLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String suitableAlternativeAccommodationGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String suitableAlternativeAccommodationTextArea;

    // Ground 10
    @CCD(label = """
            <h2>Rent arrears (ground 10)</h2>
            """,
        typeOverride = FieldType.Label)
    private String rentArrearsLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String rentArrearsGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String rentArrearsTextArea;

    // Ground 11
    @CCD(label = """
            <h2>Persistent delay in paying rent (ground 11)</h2>
            """,
        typeOverride = FieldType.Label)
    private String persistentDelayInPayingRentLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String persistentDelayInPayingRentGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String persistentDelayInPayingRentTextArea;

    // Ground 12
    @CCD(label = """
            <h2>Breach of tenancy conditions (ground 12)</h2>
            """,
        typeOverride = FieldType.Label)
    private String breachOfTenancyConditionsLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String breachOfTenancyConditionsGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String breachOfTenancyConditionsTextArea;

    // Ground 13
    @CCD(label = """
            <h2>Deterioration in the condition of the property (ground 13)</h2>
            """,
        typeOverride = FieldType.Label)
    private String propertyDeteriorationLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String propertyDeteriorationGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String propertyDeteriorationTextArea;

    // Ground 14
    @CCD(label = """
            <h2>Nuisance, annoyance, illegal or immoral use of the property (ground 14)</h2>
            """,
        typeOverride = FieldType.Label)
    private String nuisanceOrIllegalUseLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String nuisanceOrIllegalUseGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String nuisanceOrIllegalUseTextArea;

    // Ground 14A
    @CCD(label = """
            <h2>Domestic violence (ground 14A)</h2>
            """,
        typeOverride = FieldType.Label)
    private String domesticViolenceLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String domesticViolenceGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String domesticViolenceTextArea;

    // Ground 14ZA
    @CCD(label = """
            <h2>Offence during a riot (ground 14ZA)</h2>
            """,
        typeOverride = FieldType.Label)
    private String offenceDuringRiotLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String offenceDuringRiotGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String offenceDuringRiotTextArea;

    // Ground 15
    @CCD(label = """
            <h2>Deterioration of furniture (ground 15)</h2>
            """,
        typeOverride = FieldType.Label)
    private String furnitureDeteriorationLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String furnitureDeteriorationGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String furnitureDeteriorationTextArea;

    // Ground 16
    @CCD(label = """
            <h2>Employee of the landlord (ground 16)</h2>
            """,
        typeOverride = FieldType.Label)
    private String landlordEmployeeLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String landlordEmployeeGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String landlordEmployeeTextArea;

    // Ground 17
    @CCD(label = """
            <h2>Tenancy obtained by false statement (ground 17)</h2>
            """,
        typeOverride = FieldType.Label)
    private String falseStatementLabel;

    @CCD(label = """
            <h3>Why are you making a claim for possession under this ground?</h3>
            """,
        typeOverride = FieldType.Label)
    private String falseStatementGroundsLabel;

    @CCD(
        label = "Give details about your reasons for claiming possession",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on",
        access = {CaseworkerAccess.class},
        typeOverride = TextArea
    )
    private String falseStatementTextArea;
}

