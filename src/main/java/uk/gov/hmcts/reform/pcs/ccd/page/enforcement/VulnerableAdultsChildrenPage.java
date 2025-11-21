package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
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
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class VulnerableAdultsChildrenPage implements CcdPageConfiguration {
    private final TextAreaValidationService textAreaValidationService;

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
                .mandatory(EnforcementOrder::getVulnerablePeoplePresent)
                .complex(EnforcementOrder::getVulnerableAdultsChildren,
                        "vulnerablePeoplePresent=\"YES\"")
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
        List<String> errors = getValidationErrors(data);
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
    }

    private List<String> getValidationErrors(PCSCase data) {
        List<String> errors = new ArrayList<>();

        if (data.getEnforcementOrder().getVulnerablePeoplePresent() == YesNoNotSure.YES) {
            String txt = data.getEnforcementOrder().getVulnerableAdultsChildren().getVulnerableReasonText();
            errors.addAll(textAreaValidationService.validateSingleTextArea(
                txt,
                "How are they vulnerable?",
                TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
            ));
        }
        return errors;
    }
}
