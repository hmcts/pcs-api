package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimSubmitConfirmationService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitPersistenceResult;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@RequiredArgsConstructor
public class CitizenSubmissionEventStrategy implements RespondPossessionClaimSubmissionEventStrategy {

    private final DraftCaseDataService draftCaseDataService;
    private final SubmitResponseFactory submitResponseFactory;
    private final RespondPossessionClaimSubmitService respondPossessionClaimSubmitService;
    private final CounterClaimSubmitConfirmationService counterClaimSubmitConfirmationService;

    @Override
    public boolean supports(List<String> roles) {
        return roles.contains(UserRole.CITIZEN.getRole());
    }

    @Override
    public SubmitResponse<State> process(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase draftData = draftCaseDataService
            .getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        PossessionClaimResponse responseDraftData = draftData.getPossessionClaimResponse();

        Optional<SubmitResponse<State>> validationResult =
            submitResponseFactory.validate(responseDraftData, caseReference);

        if (validationResult.isPresent()) {
            return validationResult.get();
        }

        RespondPossessionClaimSubmitPersistenceResult persistenceResult =
            respondPossessionClaimSubmitService.persistFinalSubmit(caseReference, responseDraftData);

        return counterClaimSubmitConfirmationService.buildSubmitResponse(caseReference, persistenceResult);
    }
}
