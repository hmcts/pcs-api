package uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@AllArgsConstructor
@Slf4j
public class RespondToPossessionDraftSavePage implements CcdPageConfiguration {

    private final ImmutablePartyFieldValidator immutableFieldValidator;
    private final DraftCaseDataService draftCaseDataService;
    private final SecurityContextService securityContextService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("respondToPossessionDraftSavePage", this::midEvent)
            .pageLabel("Respond To Claim Event Page")
            .label("respondToPossessionDraftSavePage-info",
                   "Placeholder page to save draft data via mid event");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        long caseRef = details.getId();
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        PossessionClaimResponse defendantAnswersOnly = PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .defendantResponses(response.getDefendantResponses())
            .build();

        PCSCase partialUpdate = PCSCase.builder()
            .possessionClaimResponse(defendantAnswersOnly)
            .build();

        if (response.getDefendantContactDetails() != null
            && response.getDefendantContactDetails().getParty() != null) {

            List<String> violations = immutableFieldValidator.findImmutableFieldViolations(
                response.getDefendantContactDetails().getParty(),
                caseRef
            );

            if (!violations.isEmpty()) {
                log.error("Draft submit rejected for case {}: immutable field violations: {}", caseRef, violations);
                List<String> errors = violations.stream()
                    .map(field -> "Invalid submission: immutable field must not be sent: " + field)
                    .toList();
                return error(errors);
            }
        }

        try {
            if (securityContextService.getCurrentUserDetails().getRoles().contains(UserRole.CITIZEN.getRole())) {
                draftCaseDataService.patchUnsubmittedEventData(caseRef, partialUpdate, respondPossessionClaim);
            } else {
                UUID representedPartyId = getRequiredPartyId(caseData);
                draftCaseDataService.patchUnsubmittedEventData(
                    caseRef,
                    partialUpdate,
                    respondPossessionClaim,
                    representedPartyId
                );
            }
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(partialUpdate)
                .build();
        } catch (Exception e) {
            log.error("Failed to save draft for case {}", caseRef, e);
            return error(List.of("We couldn't save your response. Please try again or contact support."));
        }

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> error(List<String> errorMessages) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(errorMessages)
            .build();
    }

    private UUID getRequiredPartyId(PCSCase caseData) {
        String selectedPartyId = caseData.getSelectedRespondingPartyId();
        if (isBlank(selectedPartyId)) {
            throw new IllegalStateException("Missing required represented party context for respond to claim");
        }

        try {
            return UUID.fromString(selectedPartyId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid selected responding party id for respond to claim", ex);
        }
    }

}
