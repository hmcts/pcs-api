package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.TextValidatingPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;

@Component
public abstract class AbstractVulnerableAdultsChildrenPage extends TextValidatingPage {

    public static final String INFO_MARKUP = """
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
                    """;

    public static final String PAGE_LABEL = "Vulnerable adults and children at the property";

    protected AbstractVulnerableAdultsChildrenPage(TextAreaValidationService textAreaValidationService) {
        super(textAreaValidationService);
    }

    @Override
    public List<String> performValidation(PCSCase data) {
        return new ArrayList<>(getValidationErrors(getVulnerableReasonTextToValidate(data), "How are they vulnerable?",
                TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT));
    }

    public abstract String getVulnerableReasonTextToValidate(PCSCase data);

    public abstract String getVulnerablePeoplePresentShowCondition();
}
