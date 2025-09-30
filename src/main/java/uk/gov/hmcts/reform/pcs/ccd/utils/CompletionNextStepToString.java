package uk.gov.hmcts.reform.pcs.ccd.utils;

import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;

public class CompletionNextStepToString {

    public static String convert(CompletionNextStep completionNextStep) {
        return completionNextStep != null ? completionNextStep.name() : null;
    }
}
