package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PossessionGroundsValidationUtil {

    private final TextAreaValidationService textAreaValidationService;

    public static boolean hasAtLeastOneGround(Collection<?> mandatory, Collection<?> discretionary,
                                              Collection<?> other) {
        return !(CollectionUtils.isEmpty(mandatory) && CollectionUtils.isEmpty(discretionary)
                && CollectionUtils.isEmpty(other));
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> validateOtherGroundDescription(PCSCase caseData,
                                                                                       String description) {
        if (description != null) {
            List<String> validationErrors = new ArrayList<>();

            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(description,
                    PCSCase.OTHER_GROUND_DESCRIPTION_LABEL, TextAreaValidationService.MEDIUM_TEXT_LIMIT
            ));
            if (!validationErrors.isEmpty()) {
                return textAreaValidationService.createValidationResponse(caseData, validationErrors);
            }
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }
}
