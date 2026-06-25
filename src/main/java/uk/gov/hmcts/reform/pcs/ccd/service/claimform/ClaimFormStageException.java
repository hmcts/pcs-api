package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import lombok.Getter;

/**
 * Wraps a claim-form generation failure with the {@link ClaimFormStage} it occurred in, so the
 * terminal failure handler can report <em>where</em> it broke. The message and cause are carried
 * through from the underlying exception, so logging this exception still surfaces the real reason.
 */
@Getter
public class ClaimFormStageException extends RuntimeException {

    private final transient ClaimFormStage stage;

    public ClaimFormStageException(ClaimFormStage stage, Throwable cause) {
        super(cause.getMessage(), cause);
        this.stage = stage;
    }
}
