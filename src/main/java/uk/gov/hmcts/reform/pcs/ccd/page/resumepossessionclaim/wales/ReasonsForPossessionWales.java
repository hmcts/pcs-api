package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;


@AllArgsConstructor
@Component
public class ReasonsForPossessionWales implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("reasonsForPossessionWales", this::midEvent)
            .pageLabel("Reasons for possession")
            .showCondition("legislativeCountry=\"Wales\" AND showReasonsForGroundsPageWales=\"Yes\"")
            .readonly(PCSCase::getShowReasonsForGroundsPageWales, NEVER_SHOW)
            .label("reasonsForPossessionWales-separator", "---")
            .complex(PCSCase::getGroundsReasonsWales)

            // ---------- Standard/Other Contract - Discretionary grounds ----------
            .label("wales-otherBreachSection157-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Other breach of contract (section 157)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_DiscretionaryGroundsWalesCONTAINS\"OTHER_BREACH_SECTION_157\"")
            .mandatory(GroundsReasonsWales::getOtherBreachSection157Reason,
                "groundsForPossessionWales_DiscretionaryGroundsWalesCONTAINS\"OTHER_BREACH_SECTION_157\"")

            // ---------- Standard/Other Contract - Estate Management grounds ----------
            .label("wales-buildingWorks-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Building works (ground A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"BUILDING_WORKS\"")
            .mandatory(GroundsReasonsWales::getBuildingWorksReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"BUILDING_WORKS\"")

            .label("wales-redevelopmentSchemes-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Redevelopment schemes (ground B)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"REDEVELOPMENT_SCHEMES\"")
            .mandatory(GroundsReasonsWales::getRedevelopmentSchemesReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"REDEVELOPMENT_SCHEMES\"")

            .label("wales-charities-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Charities (ground C)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"CHARITIES\"")
            .mandatory(GroundsReasonsWales::getCharitiesReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"CHARITIES\"")

            .label("wales-disabledSuitableDwelling-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Dwelling suitable for disabled people (ground D)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"DISABLED_SUITABLE_DWELLING\"")
            .mandatory(GroundsReasonsWales::getDisabledSuitableDwellingReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"DISABLED_SUITABLE_DWELLING\"")

            .label("wales-housingAssociationsAndTrusts-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Housing associations and housing trusts: people difficult to house (ground E)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS"
                    + "\"HOUSING_ASSOCIATIONS_AND_TRUSTS\"")
            .mandatory(GroundsReasonsWales::getHousingAssociationsAndTrustsReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS"
                    + "\"HOUSING_ASSOCIATIONS_AND_TRUSTS\"")

            .label("wales-specialNeedsDwellings-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Groups of dwellings for people with special needs (ground F)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"SPECIAL_NEEDS_DWELLINGS\"")
            .mandatory(GroundsReasonsWales::getSpecialNeedsDwellingsReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"SPECIAL_NEEDS_DWELLINGS\"")

            .label("wales-reserveSuccessors-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Reserve successors (ground G)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"RESERVE_SUCCESSORS\"")
            .mandatory(GroundsReasonsWales::getReserveSuccessorsReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"RESERVE_SUCCESSORS\"")

            .label("wales-jointContractHolders-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Joint contract-holders (ground H)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"JOINT_CONTRACT_HOLDERS\"")
            .mandatory(GroundsReasonsWales::getJointContractHoldersReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"JOINT_CONTRACT_HOLDERS\"")

            .label("wales-otherEstateManagementReasons-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Other estate management reasons (ground I)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS"
                    + "\"OTHER_ESTATE_MANAGEMENT_REASONS\"")
            .mandatory(GroundsReasonsWales::getOtherEstateManagementReasonsReason,
                "groundsForPossessionWales_EstateManagementGroundsWalesCONTAINS\"OTHER_ESTATE_MANAGEMENT_REASONS\"")

            // ---------- Standard/Other Contract - Mandatory grounds ----------
            .label("wales-failToGiveUpS170-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Failure to give up possession on date specified in contract-holder’s notice (section 170)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_S170\"")
            .mandatory(GroundsReasonsWales::getFailToGiveUpS170Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_S170\"")

            .label("wales-landlordNoticePeriodicS178-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Landlord’s notice given in relation to periodic standard contract (section 178)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_PERIODIC_S178\"")
            .mandatory(GroundsReasonsWales::getLandlordNoticePeriodicS178Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_PERIODIC_S178\"")

            .label("wales-seriousArrearsPeriodicS181-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Contract-holder under a periodic standard contract seriously in arrears with rent (section 181)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_PERIODIC_S181\"")
            .mandatory(GroundsReasonsWales::getSeriousArrearsPeriodicS181Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_PERIODIC_S181\"")

            .label("wales-landlordNoticeFtEndS186-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Landlord’s notice in connection with end of fixed term given (section 186)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_FT_END_S186\"")
            .mandatory(GroundsReasonsWales::getLandlordNoticeFtEndS186Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_FT_END_S186\"")

            .label("wales-seriousArrearsFixedTermS187-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Contract-holder under a fixed term standard contract seriously in arrears with rent (section 187)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_FIXED_TERM_S187\"")
            .mandatory(GroundsReasonsWales::getSeriousArrearsFixedTermS187Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_FIXED_TERM_S187\"")

            .label("wales-failToGiveUpBreakNoticeS191-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Failure to give up possession on date specified in
                    contract-holder’s break clause notice (section 191)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_BREAK_NOTICE_S191\"")
            .mandatory(GroundsReasonsWales::getFailToGiveUpBreakNoticeS191Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_BREAK_NOTICE_S191\"")

            .label("wales-landlordBreakClauseS199-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Notice given under a landlord’s break clause (section 199)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"LANDLORD_BREAK_CLAUSE_S199\"")
            .mandatory(GroundsReasonsWales::getLandlordBreakClauseS199Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"LANDLORD_BREAK_CLAUSE_S199\"")

            .label("wales-convertedFixedTermSch1225B2-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Notice given in relation to end of converted fixed term standard contract
                    (paragraph 25B(2) of Schedule 12)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"CONVERTED_FIXED_TERM_SCH12_25B2\"")
            .mandatory(GroundsReasonsWales::getConvertedFixedTermSch1225B2Reason,
                "groundsForPossessionWales_MandatoryGroundsWalesCONTAINS\"CONVERTED_FIXED_TERM_SCH12_25B2\"")


            // ---------- Secure Contract - Discretionary grounds ----------
            .label("wales-secure-otherBreachOfContract-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Other breach of contract (section 157)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_DiscretionaryGroundsWalesCONTAINS\"OTHER_BREACH_OF_CONTRACT\"")
            .mandatory(GroundsReasonsWales::getSecureOtherBreachOfContractReason,
                "secureContract_DiscretionaryGroundsWalesCONTAINS\"OTHER_BREACH_OF_CONTRACT\"")

            // ---------- Secure Contract - Estate Management grounds ----------
            .label("wales-secure-buildingWorks-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Building works (ground A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"BUILDING_WORKS\"")
            .mandatory(GroundsReasonsWales::getSecureBuildingWorksReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"BUILDING_WORKS\"")

            .label("wales-secure-redevelopmentSchemes-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Redevelopment schemes (ground B)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"REDEVELOPMENT_SCHEMES\"")
            .mandatory(GroundsReasonsWales::getSecureRedevelopmentSchemesReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"REDEVELOPMENT_SCHEMES\"")

            .label("wales-secure-charities-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Charities (ground C)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"CHARITIES\"")
            .mandatory(GroundsReasonsWales::getSecureCharitiesReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"CHARITIES\"")

            .label("wales-secure-disabledSuitableDwelling-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Dwelling suitable for disabled people (ground D)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"DISABLED_SUITABLE_DWELLING\"")
            .mandatory(GroundsReasonsWales::getSecureDisabledSuitableDwellingReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"DISABLED_SUITABLE_DWELLING\"")

            .label("wales-secure-housingAssociationsAndTrusts-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Housing associations and housing trusts: people difficult to house (ground E)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"HOUSING_ASSOCIATIONS_AND_TRUSTS\"")
            .mandatory(GroundsReasonsWales::getSecureHousingAssociationsAndTrustsReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"HOUSING_ASSOCIATIONS_AND_TRUSTS\"")

            .label("wales-secure-specialNeedsDwellings-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Groups of dwellings for people with special needs (ground F)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"SPECIAL_NEEDS_DWELLINGS\"")
            .mandatory(GroundsReasonsWales::getSecureSpecialNeedsDwellingsReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"SPECIAL_NEEDS_DWELLINGS\"")

            .label("wales-secure-reserveSuccessors-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Reserve successors (ground G)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"RESERVE_SUCCESSORS\"")
            .mandatory(GroundsReasonsWales::getSecureReserveSuccessorsReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"RESERVE_SUCCESSORS\"")

            .label("wales-secure-jointContractHolders-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Joint contract-holders (ground H)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"JOINT_CONTRACT_HOLDERS\"")
            .mandatory(GroundsReasonsWales::getSecureJointContractHoldersReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"JOINT_CONTRACT_HOLDERS\"")

            .label("wales-secure-otherEstateManagementReasons-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Other estate management reasons (ground I)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_EstateManagementGroundsWalesCONTAINS\"OTHER_ESTATE_MANAGEMENT_REASONS\"")
            .mandatory(GroundsReasonsWales::getSecureOtherEstateManagementReasonsReason,
                "secureContract_EstateManagementGroundsWalesCONTAINS\"OTHER_ESTATE_MANAGEMENT_REASONS\"")

            // ---------- Secure Contract - Mandatory grounds ----------
            .label("wales-secure-failureToGiveUpPossessionSection170-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Failure to give up possession on date specified in contract-holder’s notice (section 170)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_MandatoryGroundsWalesCONTAINS\"FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170\"")
            .mandatory(GroundsReasonsWales::getSecureFailureToGiveUpPossessionSection170Reason,
                "secureContract_MandatoryGroundsWalesCONTAINS\"FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170\"")

            .label("wales-secure-landlordNoticeSection186-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Landlord’s notice in connection with end of fixed term given (section 186)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_SECTION_186\"")
            .mandatory(GroundsReasonsWales::getSecureLandlordNoticeSection186Reason,
                "secureContract_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_SECTION_186\"")

            .label("wales-secure-failureToGiveUpPossessionSection191-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Failure to give up possession on date specified in contract-holder’s break
                    clause notice (section 191)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_MandatoryGroundsWalesCONTAINS\"FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191\"")
            .mandatory(GroundsReasonsWales::getSecureFailureToGiveUpPossessionSection191Reason,
                "secureContract_MandatoryGroundsWalesCONTAINS\"FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191\"")

            .label("wales-secure-landlordNoticeSection199-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Notice given under a landlord’s break clause (section 199)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "secureContract_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_SECTION_199\"")
            .mandatory(GroundsReasonsWales::getSecureLandlordNoticeSection199Reason,
                "secureContract_MandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_SECTION_199\"")

            .done()
            .label("reasonsForPossessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        GroundsReasonsWales walesGrounds = caseData.getGroundsReasonsWales();
        if (walesGrounds != null) {
            validationErrors.addAll(validateWalesGrounds(walesGrounds));
        }

        boolean hasASB = hasASBSelected(caseData);
        caseData.setShowASBQuestionsPageWales(YesOrNo.from(hasASB));

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);

    }

    private boolean hasASBSelected(PCSCase caseData) {
        
        GroundsForPossessionWales groundsForPossessionWales =
            Optional.ofNullable(caseData.getGroundsForPossessionWales())
                .orElse(GroundsForPossessionWales.builder()
                            .discretionaryGroundsWales(Set.of())
                            .mandatoryGroundsWales(Set.of())
                            .estateManagementGroundsWales(Set.of())
                            .build());
        var discretionaryGrounds = groundsForPossessionWales.getDiscretionaryGroundsWales();


        SecureContractGroundsForPossessionWales grounds =
            Optional.ofNullable(caseData.getSecureContractGroundsForPossessionWales())
                .orElse(SecureContractGroundsForPossessionWales.builder()
                            .discretionaryGroundsWales(Set.of())
                            .mandatoryGroundsWales(Set.of())
                            .estateManagementGroundsWales(Set.of())
                            .build());

        var secureDiscretionaryGrounds = grounds.getDiscretionaryGroundsWales();

        boolean hasASBStandard = discretionaryGrounds != null
            && discretionaryGrounds.contains(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157);
        boolean hasASBSecure = secureDiscretionaryGrounds != null
            && secureDiscretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR);

        return hasASBStandard || hasASBSecure;
    }

    private List<String> validateWalesGrounds(GroundsReasonsWales grounds) {
        List<TextAreaValidationService.FieldValidation> allValidations = new ArrayList<>();
        allValidations.addAll(List.of(buildStandardDiscretionaryGroundValidations(grounds)));
        allValidations.addAll(List.of(buildEstateManagementGroundValidations(grounds)));
        allValidations.addAll(List.of(buildStandardMandatoryGroundValidations(grounds)));
        allValidations.addAll(List.of(buildSecureDiscretionaryGroundValidations(grounds)));
        allValidations.addAll(List.of(buildSecureEstateManagementGroundValidations(grounds)));
        allValidations.addAll(List.of(buildSecureMandatoryGroundValidations(grounds)));

        return textAreaValidationService.validateMultipleTextAreas(
            allValidations.toArray(new TextAreaValidationService.FieldValidation[0])
        );
    }

    private TextAreaValidationService.FieldValidation[] buildStandardMandatoryGroundValidations(
            GroundsReasonsWales grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getFailToGiveUpS170Reason(),
                MandatoryGroundWales.FAIL_TO_GIVE_UP_S170.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getLandlordNoticePeriodicS178Reason(),
                MandatoryGroundWales.LANDLORD_NOTICE_PERIODIC_S178.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSeriousArrearsPeriodicS181Reason(),
                MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getLandlordNoticeFtEndS186Reason(),
                MandatoryGroundWales.LANDLORD_NOTICE_FT_END_S186.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSeriousArrearsFixedTermS187Reason(),
                MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getFailToGiveUpBreakNoticeS191Reason(),
                MandatoryGroundWales.FAIL_TO_GIVE_UP_BREAK_NOTICE_S191.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getLandlordBreakClauseS199Reason(),
                MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getConvertedFixedTermSch1225B2Reason(),
                MandatoryGroundWales.CONVERTED_FIXED_TERM_SCH12_25B2.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildStandardDiscretionaryGroundValidations(
            GroundsReasonsWales grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getOtherBreachSection157Reason(),
                DiscretionaryGroundWales.OTHER_BREACH_SECTION_157.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildEstateManagementGroundValidations(
            GroundsReasonsWales grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getBuildingWorksReason(),
                EstateManagementGroundsWales.BUILDING_WORKS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getRedevelopmentSchemesReason(),
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getCharitiesReason(),
                EstateManagementGroundsWales.CHARITIES.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getDisabledSuitableDwellingReason(),
                EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getHousingAssociationsAndTrustsReason(),
                EstateManagementGroundsWales.HOUSING_ASSOCIATIONS_AND_TRUSTS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSpecialNeedsDwellingsReason(),
                EstateManagementGroundsWales.SPECIAL_NEEDS_DWELLINGS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getReserveSuccessorsReason(),
                EstateManagementGroundsWales.RESERVE_SUCCESSORS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getJointContractHoldersReason(),
                EstateManagementGroundsWales.JOINT_CONTRACT_HOLDERS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getOtherEstateManagementReasonsReason(),
                EstateManagementGroundsWales.OTHER_ESTATE_MANAGEMENT_REASONS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildSecureMandatoryGroundValidations(
            GroundsReasonsWales grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureFailureToGiveUpPossessionSection170Reason(),
                SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_170.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureLandlordNoticeSection186Reason(),
                SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_186.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureFailureToGiveUpPossessionSection191Reason(),
                SecureContractMandatoryGroundsWales.FAILURE_TO_GIVE_UP_POSSESSION_SECTION_191.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureLandlordNoticeSection199Reason(),
                SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_SECTION_199.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildSecureDiscretionaryGroundValidations(
            GroundsReasonsWales grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureOtherBreachOfContractReason(),
                SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }

    private TextAreaValidationService.FieldValidation[] buildSecureEstateManagementGroundValidations(
            GroundsReasonsWales grounds) {
        return new TextAreaValidationService.FieldValidation[] {
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureBuildingWorksReason(),
                EstateManagementGroundsWales.BUILDING_WORKS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureRedevelopmentSchemesReason(),
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureCharitiesReason(),
                EstateManagementGroundsWales.CHARITIES.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureDisabledSuitableDwellingReason(),
                EstateManagementGroundsWales.DISABLED_SUITABLE_DWELLING.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureHousingAssociationsAndTrustsReason(),
                EstateManagementGroundsWales.HOUSING_ASSOCIATIONS_AND_TRUSTS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureSpecialNeedsDwellingsReason(),
                EstateManagementGroundsWales.SPECIAL_NEEDS_DWELLINGS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureReserveSuccessorsReason(),
                EstateManagementGroundsWales.RESERVE_SUCCESSORS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureJointContractHoldersReason(),
                EstateManagementGroundsWales.JOINT_CONTRACT_HOLDERS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ),
            TextAreaValidationService.FieldValidation.of(
                grounds.getSecureOtherEstateManagementReasonsReason(),
                EstateManagementGroundsWales.OTHER_ESTATE_MANAGEMENT_REASONS.getLabel(),
                TextAreaValidationService.MEDIUM_TEXT_LIMIT
            )
        };
    }
}

