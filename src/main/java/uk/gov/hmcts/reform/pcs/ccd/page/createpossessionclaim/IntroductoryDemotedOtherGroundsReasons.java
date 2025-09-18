package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.IntroductoryDemotedOtherGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class IntroductoryDemotedOtherGroundsReasons implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOtherGroundsReasons")
            .pageLabel("Reasons for possession ")
            .showCondition("showIntroductoryDemotedOtherGroundReasonPage=\"Yes\""
                    + " AND (typeOfTenancyLicence=\"INTRODUCTORY_TENANCY\""
                    + " OR typeOfTenancyLicence=\"DEMOTED_TENANCY\""
                    +  " OR typeOfTenancyLicence=\"OTHER\")")
            .complex(PCSCase::getIntroductoryDemotedOtherGroundReason)
            .label("introductoryDemotedOtherGroundsReasons-antiSocial-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Antisocial behaviour</h2>
                <h3 class="govuk-heading-m" tabindex="0" >
                    Why are you making a claim for possession under this ground?
                </h3>
                """, "introductoryDemotedOrOtherGroundsCONTAINS\"ANTI_SOCIAL\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getAntiSocialBehaviourGround,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"ANTI_SOCIAL\"")

            .label("introductoryDemotedOtherGroundsReasons-breachOfTenancy-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Breach of the tenancy</h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                ""","introductoryDemotedOrOtherGroundsCONTAINS\"BREACH_OF_THE_TENANCY\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getBreachOfTenancyGround,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"BREACH_OF_THE_TENANCY\"")

            .label("introductoryDemotedOtherGroundsReasons-absoluteGrounds-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Absolute grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                ""","introductoryDemotedOrOtherGroundsCONTAINS\"ABSOLUTE_GROUNDS\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getAbsoluteGrounds,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"ABSOLUTE_GROUNDS\"")

            .label("introductoryDemotedOtherGroundsReasons-otherGround-label","""
                ---
                <h2 class="govuk-heading-l" tabindex="0">Other grounds</h2>
                <h3 class="govuk-heading-m" tabindex="0">
                    Why are you making a claim for possession under this ground?
                </h3>
                ""","introductoryDemotedOrOtherGroundsCONTAINS\"OTHER\"")
            .mandatory(IntroductoryDemotedOtherGroundReason::getOtherGround,
                    "introductoryDemotedOrOtherGroundsCONTAINS\"OTHER\"")
                .done();

    }
}
