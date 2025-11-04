package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.PeriodicContractTermsWalesTest;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren.VULNERABLE_REASON_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren.VULNERABLE_REASON_TEXT_LIMIT;

public class VulnerableAdultsChildrenPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("vulnerableAdultsChildren", this::midEvent)
            .pageLabel("Vulnerable adults and children at the property")
            .label("vulnerableAdultsChildren-line-separator", "---")
            .label(
                "vulnerableAdultsChildren-information-text", """
                    <p class="govuk-body govuk-!-font-weight-bold">
                        The bailiff needs to know if anyone at the property is vulnerable.
                    </p>
                    <p class="govuk-body govuk-!-margin-bottom-0">Someone is vulnerable if they have:</p>
                    <ul>
                        <li class="govuk-!-font-size-19">a history of drug or alcohol abuse</li>
                        <li class="govuk-!-font-size-19">a mental health condition</li>
                        <li class="govuk-!-font-size-19">a disability, for example a learning disability or
                            cognitive impairment</li>
                        <li class="govuk-!-font-size-19">been a victim of domestic abuse</li>
                    </ul>
                    """
            )
            .label("branch2506-pattern-separator", "---")
            .label(
                "branch2506-pattern-header", """
                    <h2 class="govuk-heading-m">EXACT BRANCH 2506 PATTERN (ProhibitedConductWales Structure)</h2>
                    """
            )
            .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getTestProhibitedConductClaim)
                .complex(EnforcementOrder::getPeriodicContractTermsWalesTest, 
                         "testProhibitedConductClaim=\"YES\"")
                    .mandatory(PeriodicContractTermsWalesTest::getAgreedTermsOfPeriodicContract)
                    .mandatory(
                        PeriodicContractTermsWalesTest::getDetailsOfTerms, 
                        "periodicContractTermsWalesTest.agreedTermsOfPeriodicContract=\"VULNERABLE_ADULTS\" "
                        + "OR periodicContractTermsWalesTest.agreedTermsOfPeriodicContract=\"VULNERABLE_CHILDREN\" "
                        + " OR periodicContractTermsWalesTest.agreedTermsOfPeriodicContract=\"VULNERABLE_ADULTS_AND_CHILDREN\""
                    )
                .done()
            .done()
            .label("vulnerableAdultsChildren-saveAndReturn", CommonPageContent.SAVE_AND_RETURN)


            .label("jsonUnwrapped-pattern-separator", "---")
            .label(
                "jsonUnwrapped-pattern-header", """
                    <h2 class="govuk-heading-m">JSON UNWRAPPED PATTERN (VulnerableAdultsChildren with @JsonUnwrapped)</h2>
                    """
            )
            .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getVulnerablePeopleYesNo)
                .complex(EnforcementOrder::getVulnerableAdultsChildren, "vulnerablePeopleYesNo=\"YES\"")
                    .mandatory(VulnerableAdultsChildren::getVulnerableCategory, "vulnerablePeopleYesNo=\"YES\"")
                    .mandatory(
                        VulnerableAdultsChildren::getVulnerableReasonText
                    )
                .done()
            .done()
            .label("vulnerableAdultsChildren-saveAndReturn3", CommonPageContent.SAVE_AND_RETURN);
            
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        VulnerableAdultsChildren vulnerableAdultsChildren = data.getEnforcementOrder().getVulnerableAdultsChildren();
        // Only validate when user selected YES
        if (data.getEnforcementOrder().getVulnerablePeopleYesNo() == YesNoNotSure.YES) {
            String txt = vulnerableAdultsChildren.getVulnerableReasonText();
            // TODO: Use TextAreaValidationService from PR #751 when merged
            if (txt.length() > VULNERABLE_REASON_TEXT_LIMIT) {
                errors.add(EnforcementValidationUtil
                        .getCharacterLimitErrorMessage(VULNERABLE_REASON_LABEL,
                                VULNERABLE_REASON_TEXT_LIMIT));
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
    }
}
