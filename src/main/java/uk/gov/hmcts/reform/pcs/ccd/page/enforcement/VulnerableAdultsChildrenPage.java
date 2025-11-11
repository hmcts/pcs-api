package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

import java.util.ArrayList;
import java.util.List;

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
                    <ul class="govuk-list govuk-list--bullet" style="color: #0b0c0c;">
                        <li class="govuk-!-font-size-19">a history of drug or alcohol abuse</li>
                        <li class="govuk-!-font-size-19">a mental health condition</li>
                        <li class="govuk-!-font-size-19">a disability, for example a learning disability or
                            cognitive impairment</li>
                        <li class="govuk-!-font-size-19">been a victim of domestic abuse</li>
                    </ul>
                    """
            )
            .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getVulnerablePeopleYesNo)
                .complex(EnforcementOrder::getVulnerableAdultsChildren, 
                        "vulnerablePeopleYesNo=\"YES\"")
                    .mandatory(VulnerableAdultsChildren::getVulnerableCategory)
                    .mandatory(
                        VulnerableAdultsChildren::getVulnerableReasonText,
                        "vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_ADULTS\" "
                            + "OR vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_CHILDREN\" "
                            + "OR vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_ADULTS_AND_CHILDREN\""
                    )
                .done()
            .done()
            .label("vulnerableAdultsChildren-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        // Only validate when user selected YES
        if (data.getEnforcementOrder().getVulnerablePeopleYesNo() == YesNoNotSure.YES) {
            String txt = data.getEnforcementOrder().getVulnerableAdultsChildren().getVulnerableReasonText();
            int limit = EnforcementRiskValidationUtils.getCharacterLimit();
            if (txt.length() > limit) {
                // TODO: Use TextAreaValidationService from PR #751 when merged
                errors.add(EnforcementValidationUtil
                        .getCharacterLimitErrorMessage("How are they vulnerable?", limit));
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
    }
}
