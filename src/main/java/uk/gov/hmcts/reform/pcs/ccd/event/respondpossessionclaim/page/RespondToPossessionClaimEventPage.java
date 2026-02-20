package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.page;

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
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class RespondToPossessionClaimEventPage implements CcdPageConfiguration {


    private final ImmutablePartyFieldValidator immutableFieldValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("respondToPossessionClaimEventPage", this::midEvent)
            .pageLabel("Respond To Claim Event Page")
            .label("Placeholder", "Placeholder Page to save draft data via mid event");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        Long caseRef = details.getId();
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
                log.error(
                    "Draft submit rejected for case {}: immutable field violations: {}",
                    caseRef, violations
                );

                List<String> errors = violations.stream()
                    .map(field -> "Invalid submission: immutable field must not be sent: " + field)
                    .toList();

                return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(errors)
                    .build();
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(partialUpdate)
            .build();
    }

}
