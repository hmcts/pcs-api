package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy.RespondPossessionClaimSubmissionEventStrategy;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

@Component("respondToClaimSubmitEventHandler")
@Slf4j
@RequiredArgsConstructor
public class SubmitEventHandler implements Submit<PCSCase, State> {

    private final List<RespondPossessionClaimSubmissionEventStrategy> strategies;
    private final SecurityContextService securityContextService;

    @Override
    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        log.info("RespondPossessionClaim submit callback invoked for Case Reference: {}", caseReference);

        return strategies.stream()
            .filter(strategy -> strategy.supports(securityContextService.getCurrentUserDetails().getRoles()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No submit event strategy found"))
            .process(eventPayload);
    }
}
