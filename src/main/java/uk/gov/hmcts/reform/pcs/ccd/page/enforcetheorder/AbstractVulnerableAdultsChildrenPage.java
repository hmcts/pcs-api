package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.page.AbstractPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.List;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public abstract class AbstractVulnerableAdultsChildrenPage extends AbstractPage {

    protected final TextAreaValidationService textAreaValidationService;

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

    public abstract String getVulnerablePeoplePresentShowCondition();

    public final Predicate<YesNoNotSure> vulnerablePeoplePresent = v -> v == YesNoNotSure.YES;

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = performValidation(data);
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
                .build();
    }

    public List<String> getValidationErrors(String txt) {
        return textAreaValidationService.validateSingleTextArea(
                txt,
                "How are they vulnerable?",
                TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT);
    }

    public abstract List<String> performValidation(PCSCase data);
}
