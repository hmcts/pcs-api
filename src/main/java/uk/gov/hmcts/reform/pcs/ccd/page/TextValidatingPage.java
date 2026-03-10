package uk.gov.hmcts.reform.pcs.ccd.page;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;

import java.util.List;

@Component
public abstract class TextValidatingPage implements CcdPage {

    private final TextAreaValidationService textAreaValidationService;

    protected TextValidatingPage(TextAreaValidationService textAreaValidationService) {
        this.textAreaValidationService = textAreaValidationService;
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = performValidation(data);
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
                .build();
    }

    public abstract List<String> performValidation(PCSCase data);

    public List<String> getValidationErrors(String txt, String message, int maxLength) {
        return textAreaValidationService.validateSingleTextArea(txt, message, maxLength);
    }
}
