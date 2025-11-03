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
                    <p class="govuk-body govuk-!-font-weight-bold">The bailiff needs to know if anyone at the property is vulnerable.</p>
                    <p class="govuk-body govuk-!-margin-bottom-0">Someone is vulnerable if they have:</p>
                    <ul>
                        <li>a history of drug or alcohol abuse</li>
                        <li>a mental health condition</li>
                        <li>a disability, for example a learning disability or cognitive impairment</li>
                        <li>been a victim of domestic abuse</li>
                    </ul>
                    """
            )
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getVulnerableAdultsChildren)
            .mandatory(VulnerableAdultsChildren::getVulnerablePeopleYesNo)
            .mandatory(VulnerableAdultsChildren::getVulnerableCategory,
                    ShowConditions.fieldEquals("vulnerableAdultsChildren.vulnerablePeopleYesNo", YesNoNotSure.YES))
            .mandatory(VulnerableAdultsChildren::getVulnerableReasonText,
                    ShowConditions.fieldEquals("vulnerableAdultsChildren.vulnerablePeopleYesNo", YesNoNotSure.YES)
                            + " AND (vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_ADULTS\" "
                            + "OR vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_CHILDREN\" "
                            + "OR vulnerableAdultsChildren.vulnerableCategory=\"VULNERABLE_ADULTS_AND_CHILDREN\")")
            .done()
            .label("vulnerableAdultsChildren-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        String txt = data.getEnforcementOrder().getVulnerableAdultsChildren().getVulnerableReasonText();

        // TODO: Use TextAreaValidationService from PR #751 when merged
        if (txt.length() > VULNERABLE_REASON_TEXT_LIMIT) {
            errors.add(EnforcementValidationUtil
                    .getCharacterLimitErrorMessage(VULNERABLE_REASON_LABEL,
                            VULNERABLE_REASON_TEXT_LIMIT));
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
    }
}
