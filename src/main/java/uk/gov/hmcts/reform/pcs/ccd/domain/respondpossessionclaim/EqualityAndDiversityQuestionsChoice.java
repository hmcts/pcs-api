package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum EqualityAndDiversityQuestionsChoice implements HasLabel {

    CONTINUE("Continue"),
    SKIP("Skip");

    private final String label;
}
