package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy.RespondPossessionClaimStartEventStrategy;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

/**
 * Start event handler for RespondPossessionClaim.
 *
 * <p>Two flows:
 * - Citizen journey: Create draft or load existing draft for CITIZEN User role.
 * - Legal Representative journey: Create draft per active linked defendants or load specific defendant draft.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final SecurityContextService securityContextService;
    private final List<RespondPossessionClaimStartEventStrategy> strategies;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {

        long caseReference = eventPayload.caseReference();

        log.info(
            "RespondPossessionClaim start callback invoked for Case Reference: {}",
            caseReference
        );

        boolean citizenUser = securityContextService.getCurrentUserDetails().getRoles()
            .contains(UserRole.CITIZEN.getRole());

        return strategies.stream()
            .filter(strategy -> strategy.supports(citizenUser))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No start event strategy found"))
            .loadDraft(caseReference, eventPayload.caseData());
    }
}
