package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.api.ShowCondition.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure.YES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory.VULNERABLE_ADULTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory.VULNERABLE_ADULTS_AND_CHILDREN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.VulnerableCategory.VULNERABLE_CHILDREN;

@AllArgsConstructor
@Component
public class VulnerableAdultsChildrenPage implements CcdPageConfiguration {
    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("vulnerableAdultsChildren", this::midEvent)
            .pageLabel("Vulnerable adults and children at the property")
            .showWhen(ShowConditionsEnforcementType.WARRANT_FLOW)
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
            .complex(EnforcementOrder::getRawWarrantDetails)
            .mandatory(RawWarrantDetails::getVulnerablePeoplePresent)
            .complexWhen(RawWarrantDetails::getVulnerableAdultsChildren,
                when(EnforcementOrder::getRawWarrantDetails, RawWarrantDetails::getVulnerablePeoplePresent).is(YES))
            .mandatory(VulnerableAdultsChildren::getVulnerableCategory)
            .mandatoryWhen(VulnerableAdultsChildren::getVulnerableReasonText,
                when(EnforcementOrder::getRawWarrantDetails, RawWarrantDetails::getVulnerablePeoplePresent).is(YES)
                    .and(when(RawWarrantDetails::getVulnerableAdultsChildren,
                        VulnerableAdultsChildren::getVulnerableCategory).isAnyOf(
                            VULNERABLE_ADULTS, VULNERABLE_CHILDREN, VULNERABLE_ADULTS_AND_CHILDREN)))
            .done()
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
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .build();
    }

    private List<String> getValidationErrors(PCSCase data) {
        List<String> errors = new ArrayList<>();

        if (data.getEnforcementOrder().getRawWarrantDetails().getVulnerablePeoplePresent() == YesNoNotSure.YES) {
            String txt = data.getEnforcementOrder()
                    .getRawWarrantDetails().getVulnerableAdultsChildren() != null
                    ? data.getEnforcementOrder()
                        .getRawWarrantDetails().getVulnerableAdultsChildren().getVulnerableReasonText()
                    : null;
            errors.addAll(textAreaValidationService.validateSingleTextArea(
                txt,
                "How are they vulnerable?",
                TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
            ));
        }
        return errors;
    }
}
