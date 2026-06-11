package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.LegalRepresentativeRetriever;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@RequiredArgsConstructor
public class LegalRepSubmissionEventStrategy implements RespondPossessionClaimSubmissionEventStrategy {

    private final DraftCaseDataService draftCaseDataService;
    private final ClaimResponseService claimResponseService;
    private final DefendantResponseService defendantResponseService;
    private final SelectedPartyRetriever selectedPartyRetriever;
    private final SubmitResponseFactory submitResponseFactory;
    private final LegalRepresentativeRetriever legalRepresentativeRetriever;
    private final OrganisationDetailsService organisationDetailsService;
    private final SecurityContextService securityContextService;

    @Override
    public boolean supports(List<String> roles) {
        return !roles.contains(UserRole.CITIZEN.getRole());
    }

    @Transactional
    @Override
    public SubmitResponse<State> process(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        UUID representedPartyId = selectedPartyRetriever
            .getCurrentRepresentedPartyId(eventPayload.caseData())
            .orElseThrow(() -> new IllegalStateException("No selected responding party id for respond to claim"));

        String organisationId = organisationDetailsService
            .getOrganisationIdentifier(securityContextService.getCurrentUserId().toString());

        UUID legalRepOrganisationIdForUser = legalRepresentativeRetriever.getLegalRepOrganisationIdForUser(
            caseReference,
            organisationId
        );

        PCSCase draftData = draftCaseDataService
            .getUnsubmittedCaseData(caseReference, respondPossessionClaim, representedPartyId,
                                    legalRepOrganisationIdForUser)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        PossessionClaimResponse responseDraftData = draftData.getPossessionClaimResponse();

        Optional<SubmitResponse<State>> validationResult = submitResponseFactory
            .validate(responseDraftData, caseReference);

        if (validationResult.isPresent()) {
            return validationResult.get();
        }

        claimResponseService.saveDraftDataForParty(responseDraftData, caseReference, representedPartyId);
        defendantResponseService.saveDefendantResponse(caseReference, responseDraftData, representedPartyId);
        draftCaseDataService.deleteUnsubmittedCaseData(caseReference, respondPossessionClaim, representedPartyId,
                                                       legalRepOrganisationIdForUser);

        return submitResponseFactory.success();
    }
}
