package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class GroundsReasonsWales {

    // ---------- Standard/Other Contract - Mandatory grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Failure to give up possession on date "
            + "specified in contract-holder’s notice (section 170))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String failToGiveUpS170Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Landlord’s notice given in relation "
            + "to periodic standard contract (section 178))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String landlordNoticePeriodicS178Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Contract-holder under a periodic "
            + "standard contract seriously in arrears with rent (section 181))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String seriousArrearsPeriodicS181Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Landlord’s notice in connection "
            + "with end of fixed term given (section 186))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String landlordNoticeFtEndS186Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Contract-holder under a fixed "
            + "term standard contract seriously in arrears with rent (section 187))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String seriousArrearsFixedTermS187Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Failure to give up possession "
            + "on date specified in contract-holder’s break clause notice (section 191))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String failToGiveUpBreakNoticeS191Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Notice given under a landlord’s "
            + "break clause (section 199))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String landlordBreakClauseS199Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Notice given in relation to end "
            + "of converted fixed term standard contract (paragraph 25B(2) of Schedule 12))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String convertedFixedTermSch1225B2Reason;

    // ---------- Standard/Other Contract - Discretionary grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Other breach of contract (section 157))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String otherBreachSection157Reason;

    // ---------- Standard/Other Contract -  Estate Management grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Building works (ground A))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String buildingWorksReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Redevelopment schemes (ground B))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String redevelopmentSchemesReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Charities (ground C))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String charitiesReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Dwelling suitable for disabled "
            + "people (ground D))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String disabledSuitableDwellingReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Housing associations and housing "
            + "trusts: people difficult to house (ground E))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String housingAssociationsAndTrustsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Groups of dwellings for people "
            + "with special needs (ground F))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String specialNeedsDwellingsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Reserve successors (ground G))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String reserveSuccessorsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Joint contract-holders (ground H))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String jointContractHoldersReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Other estate management reasons (ground I))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String otherEstateManagementReasonsReason;

    
    // ---------- Secure Contract - Mandatory grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Failure to give up possession on date "
            + "specified in contract-holder’s notice (section 170))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureFailureToGiveUpPossessionSection170Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Landlord’s notice in connection "
            + "with end of fixed term given (section 186))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureLandlordNoticeSection186Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Failure to give up possession on date "
            + "specified in contract-holder’s break clause notice (section 191))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureFailureToGiveUpPossessionSection191Reason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Notice given under a landlord’s "
            + "break clause (section 199))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureLandlordNoticeSection199Reason;

    // ---------- Secure Contract - Discretionary grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Other breach of contract (section 157))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureOtherBreachOfContractReason;

    // ---------- Secure Contract - Estate Management grounds ----------
    @CCD(
        label = "Give details about your reasons for claiming possession (Building works (ground A))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureBuildingWorksReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Redevelopment schemes (ground B))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureRedevelopmentSchemesReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Charities (ground C))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureCharitiesReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Dwelling suitable for disabled "
            + "people (ground D))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureDisabledSuitableDwellingReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Housing associations and housing "
            + "trusts: people difficult to house (ground E))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureHousingAssociationsAndTrustsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Groups of dwellings for people "
            + "with special needs (ground F))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureSpecialNeedsDwellingsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Reserve successors (ground G))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureReserveSuccessorsReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Joint contract-holders (ground H))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureJointContractHoldersReason;

    @CCD(
        label = "Give details about your reasons for claiming possession (Other estate management reasons (ground I))",
        hint = "You’ll be able to upload documents to support or further explain your reasons later on. "
            + "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String secureOtherEstateManagementReasonsReason;

}

