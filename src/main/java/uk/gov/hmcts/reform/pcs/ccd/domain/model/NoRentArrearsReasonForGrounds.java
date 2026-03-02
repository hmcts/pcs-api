package uk.gov.hmcts.reform.pcs.ccd.domain.model;

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
public class NoRentArrearsReasonForGrounds {

    // Ground 1
    @CCD(
            label = "Give details about your reasons for claiming possession (Owner occupier (ground 1))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String ownerOccupierTextArea;

    // Ground 2
    @CCD(
            label = "Give details about your reasons for claiming possession (Repossession by the landlord’s"
                + " mortgage lender (ground 2))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String repossessionByLenderTextArea;

    // Ground 3
    @CCD(
        label = "Give details about your reasons for claiming possession (Holiday let (ground 3))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String holidayLetTextArea;

    // Ground 4
    @CCD(
            label = "Give details about your reasons for claiming possession (Student let (ground 4))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String studentLetTextArea;

    // Ground 5
    @CCD(
            label = "Give details about your reasons for claiming possession (Property required for minister of"
            + " religion (ground 5))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String ministerOfReligionTextArea;

    // Ground 6
    @CCD(
            label = "Give details about your reasons for claiming possession (Property required for "
                + "redevelopment (ground 6))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String redevelopmentTextArea;

    // Ground 7
    @CCD(
            label = "Give details about your reasons for claiming possession (Death of the tenant (ground 7))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String deathOfTenantTextArea;

    // Ground 7A
    @CCD(
            label = "Give details about your reasons for claiming possession (Antisocial behaviour (ground 7A))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String antisocialBehaviourTextArea;

    // Ground 7B
    @CCD(
            label = "Give details about your reasons for claiming possession (Tenant does not have a right "
                + "to rent (ground 7B))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String noRightToRentTextArea;

    // Ground 9
    @CCD(
            label = "Give details about your reasons for claiming possession (Suitable alternative"
                + " accommodation (ground 9))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String suitableAccomTextArea;

    // Ground 12
    @CCD(
            label = "Give details about your reasons for claiming possession "
                + "(Breach of tenancy conditions (ground 12))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String breachOfTenancyConditionsTextArea;

    // Ground 13
    @CCD(
            label = "Give details about your reasons for claiming possession (Deterioration in the condition "
                + "of the property (ground 13))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String propertyDeteriorationTextArea;

    // Ground 14
    @CCD(
            label = "Give details about your reasons for claiming possession (Nuisance, annoyance, illegal or "
                + "immoral use of the property (ground 14))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String nuisanceOrIllegalUseTextArea;

    // Ground 14A
    @CCD(
            label = "Give details about your reasons for claiming possession (Domestic violence (ground 14A))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String domesticViolenceTextArea;

    // Ground 14ZA
    @CCD(
            label = "Give details about your reasons for claiming possession (Offence during a riot (ground 14ZA))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String offenceDuringRiotTextArea;

    // Ground 15
    @CCD(
            label = "Give details about your reasons for claiming possession (Deterioration of furniture (ground 15))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String furnitureDeteriorationTextArea;

    // Ground 16
    @CCD(
            label = "Give details about your reasons for claiming possession (Employee of the landlord (ground 16))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String landlordEmployeeTextArea;

    // Ground 17
    @CCD(
            label = "Give details about your reasons for claiming possession (Tenancy obtained by false "
                + "statement (ground 17))",
            hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
                + "You can enter up to 500 characters",
            typeOverride = TextArea
    )
    private String falseStatementTextArea;
}
