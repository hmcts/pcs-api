package uk.gov.hmcts.reform.pcs.ccd.service;

import uk.gov.hmcts.ccd.sdk.ActorAttribution;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionContext;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;

import java.util.UUID;

/** Runs each system event's action straight away, for unit tests of the work done inside the event. */
public class ImmediateSystemEventExecutor implements SystemEventExecutor {

    @Override
    public SystemEventExecutionResult execute(long caseReference, UUID idempotencyKey, SystemEventAction action) {
        action.execute(new SystemEventExecutionContext(caseReference, idempotencyKey, "PCS", "CASE_ISSUED"));
        return new SystemEventExecutionResult(1L, SystemEventExecutionResult.Outcome.EXECUTED);
    }

    @Override
    public SystemEventExecutionResult execute(long caseReference, ActorAttribution actor, UUID idempotencyKey,
                                              SystemEventAction action) {
        return execute(caseReference, idempotencyKey, action);
    }
}
