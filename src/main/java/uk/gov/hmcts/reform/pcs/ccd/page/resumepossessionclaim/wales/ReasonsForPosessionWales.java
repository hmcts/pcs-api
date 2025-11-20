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
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;


@AllArgsConstructor
@Component
public class ReasonsForPosessionWales implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("reasonsForPosessionWales", this::midEvent)
            .pageLabel("Reasons for possession")
            .showCondition("legislativeCountry=\"Wales\" AND showReasonsForGroundsPageWales=\"Yes\"")
            .readonly(PCSCase::getShowReasonsForGroundsPageWales, NEVER_SHOW)
            .label("reasonsForPosessionWales-separator", "---")
            .complex(PCSCase::getGroundsReasonsWales)

            // ---------- Standard/Other Contract - Mandatory grounds ----------
            .label("wales-failToGiveUpS170-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Failure to give up possession on date specified in contract-holder's notice (section 170)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_S170\"")
            .mandatory(GroundsReasonsWales::getFailToGiveUpS170Reason,
                "mandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_S170\"")

            .label("wales-landlordNoticePeriodicS178-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Landlord's notice given in relation to periodic standard contract (section 178)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_PERIODIC_S178\"")
            .mandatory(GroundsReasonsWales::getLandlordNoticePeriodicS178Reason,
                "mandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_PERIODIC_S178\"")

            .label("wales-seriousArrearsPeriodicS181-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Contract-holder under a periodic standard contract seriously in arrears with rent (section 181)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_PERIODIC_S181\"")
            .mandatory(GroundsReasonsWales::getSeriousArrearsPeriodicS181Reason,
                "mandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_PERIODIC_S181\"")

            .label("wales-landlordNoticeFtEndS186-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Landlord's notice in connection with end of fixed term given (section 186)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_FT_END_S186\"")
            .mandatory(GroundsReasonsWales::getLandlordNoticeFtEndS186Reason,
                "mandatoryGroundsWalesCONTAINS\"LANDLORD_NOTICE_FT_END_S186\"")

            .label("wales-seriousArrearsFixedTermS187-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Contract-holder under a fixed term standard contract seriously in arrears with rent (section 187)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_FIXED_TERM_S187\"")
            .mandatory(GroundsReasonsWales::getSeriousArrearsFixedTermS187Reason,
                "mandatoryGroundsWalesCONTAINS\"SERIOUS_ARREARS_FIXED_TERM_S187\"")

            .label("wales-failToGiveUpBreakNoticeS191-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Failure to give up possession on date specified in 
                    contract-holder's break clause notice (section 191)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_BREAK_NOTICE_S191\"")
            .mandatory(GroundsReasonsWales::getFailToGiveUpBreakNoticeS191Reason,
                "mandatoryGroundsWalesCONTAINS\"FAIL_TO_GIVE_UP_BREAK_NOTICE_S191\"")

            .label("wales-landlordBreakClauseS199-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Notice given under a landlord's break clause (section 199)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"LANDLORD_BREAK_CLAUSE_S199\"")
            .mandatory(GroundsReasonsWales::getLandlordBreakClauseS199Reason,
                "mandatoryGroundsWalesCONTAINS\"LANDLORD_BREAK_CLAUSE_S199\"")

            .label("wales-convertedFixedTermSch1225B2-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Notice given in relation to end of converted fixed term standard contract 
                    (paragraph 25B(2) of Schedule 12)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "mandatoryGroundsWalesCONTAINS\"CONVERTED_FIXED_TERM_SCH12_25B2\"")
            .mandatory(GroundsReasonsWales::getConvertedFixedTermSch1225B2Reason,
                "mandatoryGroundsWalesCONTAINS\"CONVERTED_FIXED_TERM_SCH12_25B2\"")

            // ---------- Standard/Other Contract - Discretionary grounds ----------
            .label("wales-otherBreachSection157-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Other breach of contract (section 157)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "discretionaryGroundsWalesCONTAINS\"OTHER_BREACH_SECTION_157\"")
            .mandatory(GroundsReasonsWales::getOtherBreachSection157Reason,
                "discretionaryGroundsWalesCONTAINS\"OTHER_BREACH_SECTION_157\"")

            // ---------- Estate Management grounds ----------
            .label("wales-buildingWorks-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Building works (ground A)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"BUILDING_WORKS\"")
            .mandatory(GroundsReasonsWales::getBuildingWorksReason,
                "estateManagementGroundsWalesCONTAINS\"BUILDING_WORKS\"")

            .label("wales-redevelopmentSchemes-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Redevelopment schemes (ground B)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"REDEVELOPMENT_SCHEMES\"")
            .mandatory(GroundsReasonsWales::getRedevelopmentSchemesReason,
                "estateManagementGroundsWalesCONTAINS\"REDEVELOPMENT_SCHEMES\"")

            .label("wales-charities-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Charities (ground C)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"CHARITIES\"")
            .mandatory(GroundsReasonsWales::getCharitiesReason,
                "estateManagementGroundsWalesCONTAINS\"CHARITIES\"")

            .label("wales-disabledSuitableDwelling-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Dwelling suitable for disabled people (ground D)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"DISABLED_SUITABLE_DWELLING\"")
            .mandatory(GroundsReasonsWales::getDisabledSuitableDwellingReason,
                "estateManagementGroundsWalesCONTAINS\"DISABLED_SUITABLE_DWELLING\"")

            .label("wales-housingAssociationsAndTrusts-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Housing associations and housing trusts: people difficult to house (ground E)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"HOUSING_ASSOCIATIONS_AND_TRUSTS\"")
            .mandatory(GroundsReasonsWales::getHousingAssociationsAndTrustsReason,
                "estateManagementGroundsWalesCONTAINS\"HOUSING_ASSOCIATIONS_AND_TRUSTS\"")

            .label("wales-specialNeedsDwellings-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Groups of dwellings for people with special needs (ground F)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"SPECIAL_NEEDS_DWELLINGS\"")
            .mandatory(GroundsReasonsWales::getSpecialNeedsDwellingsReason,
                "estateManagementGroundsWalesCONTAINS\"SPECIAL_NEEDS_DWELLINGS\"")

            .label("wales-reserveSuccessors-label","""
                <h2 class="govuk-heading-l" tabindex="0">
                    Reserve successors (ground G)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"RESERVE_SUCCESSORS\"")
            .mandatory(GroundsReasonsWales::getReserveSuccessorsReason,
                "estateManagementGroundsWalesCONTAINS\"RESERVE_SUCCESSORS\"")

            .label("wales-jointContractHolders-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Joint contract-holders (ground H)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"JOINT_CONTRACT_HOLDERS\"")
            .mandatory(GroundsReasonsWales::getJointContractHoldersReason,
                "estateManagementGroundsWalesCONTAINS\"JOINT_CONTRACT_HOLDERS\"")

            .label("wales-otherEstateManagementReasons-label", """
                <h2 class="govuk-heading-l" tabindex="0">
                    Other estate management reasons (ground I)
                </h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "estateManagementGroundsWalesCONTAINS\"OTHER_ESTATE_MANAGEMENT_REASONS\"")
            .mandatory(GroundsReasonsWales::getOtherEstateManagementReasonsReason,
                "estateManagementGroundsWalesCONTAINS\"OTHER_ESTATE_MANAGEMENT_REASONS\"")

            .done()
            .label("reasonsForPosessionWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        boolean hasASB = hasASBSelected(caseData);
        if (hasASB) {
            caseData.setShowASBQuestionsPageWales(YesOrNo.YES);
        } else {
            caseData.setShowASBQuestionsPageWales(YesOrNo.NO);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private boolean hasASBSelected(PCSCase caseData) {
        var discretionaryGrounds = caseData.getDiscretionaryGroundsWales();
        var secureDiscretionaryGrounds = caseData.getSecureContractDiscretionaryGroundsWales();

        boolean hasASBStandard = discretionaryGrounds != null
                && discretionaryGrounds.contains(DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157);
        boolean hasASBSecure = secureDiscretionaryGrounds != null
                && secureDiscretionaryGrounds.contains(SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR);

        return hasASBStandard || hasASBSecure;
    }
}
