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
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String ownerOccupierTextArea;

    // Ground 2
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String repossessionByLenderTextArea;

    // Ground 3
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String holidayLetTextArea;

    // Ground 4
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String studentLetTextArea;

    // Ground 5
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String ministerOfReligionTextArea;

    // Ground 6
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String redevelopmentTextArea;

    // Ground 7
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String deathOfTenantTextArea;

    // Ground 7A
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String antisocialBehaviourTextArea;

    // Ground 7B
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String noRightToRentTextArea;

    // Ground 8
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String seriousRentArrearsTextArea;

    // Ground 9
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String suitableAccomTextArea;

    // Ground 10
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String rentArrearsTextArea;

    // Ground 11
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String rentPaymentDelayTextArea;

    // Ground 12
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String breachOfTenancyConditionsTextArea;

    // Ground 13
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String propertyDeteriorationTextArea;

    // Ground 14
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String nuisanceOrIllegalUseTextArea;

    // Ground 14A
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String domesticViolenceTextArea;

    // Ground 14ZA
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String offenceDuringRiotTextArea;

    // Ground 15
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String furnitureDeteriorationTextArea;

    // Ground 16
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String landlordEmployeeTextArea;

    // Ground 17
    @CCD(
            label = "Give details about your reasons for claiming possession",
            hint = "You'll be able to upload documents to support or further explain your reasons later on",
            typeOverride = TextArea
    )
    private String falseStatementTextArea;
}
