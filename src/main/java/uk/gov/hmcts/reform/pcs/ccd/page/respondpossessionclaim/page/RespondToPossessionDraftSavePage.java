package uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@AllArgsConstructor
@Slf4j
public class RespondToPossessionDraftSavePage implements CcdPageConfiguration {

    private final DraftCaseDataService draftCaseDataService;

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
        final long caseRef = details.getId();
        PossessionClaimResponse response = caseData.getPossessionClaimResponse();

        PossessionClaimResponse defendantAnswersOnly = PossessionClaimResponse.builder()
            .defendantContactDetails(response.getDefendantContactDetails())
            .defendantResponses(response.getDefendantResponses())
            .build();

        PCSCase partialUpdate = PCSCase.builder()
            .possessionClaimResponse(defendantAnswersOnly)
            .build();

        try {
            draftCaseDataService.saveUnsubmittedEventData(caseRef, partialUpdate, respondPossessionClaim);
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

}
