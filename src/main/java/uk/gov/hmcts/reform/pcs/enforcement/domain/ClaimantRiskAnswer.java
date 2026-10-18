package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * One risk assessment question with the claimant's original answer and, if they changed it, their updated
 * answer. Set for the case view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantRiskAnswer {

    @CCD(label = "Question")
    private String question;

    @CCD(label = "Original answer", typeOverride = TextArea)
    private String originalAnswer;

    /**
     * Empty when the claimant has not changed the answer.
     */
    @CCD(label = "Updated answer", typeOverride = TextArea)
    private String updatedAnswer;

    @CCD(label = "Answer changed")
    private YesOrNo answerChanged;
}
