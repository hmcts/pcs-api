package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Service
@Slf4j
@RequiredArgsConstructor
public class RespondPossessionClaimDraftService {

    private final DraftCaseDataService draftCaseDataService;

    public boolean exists(long caseReference) {
        return draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim);
    }

    public PCSCase load(long caseReference) {
        log.info("Loading existing draft for case {} and event {}", caseReference, respondPossessionClaim);

        PCSCase draftData = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new IllegalStateException("Draft not found for case " + caseReference));

        return PCSCase.builder()
            .possessionClaimResponse(draftData.getPossessionClaimResponse())
            .submitDraftAnswers(YesOrNo.NO)
            .build();
    }

    public PCSCase initialize(long caseReference, PossessionClaimResponse initialResponse) {
        PCSCase filteredDraft = PCSCase.builder()
            .possessionClaimResponse(initialResponse)
            .build();

        draftCaseDataService.patchUnsubmittedEventData(caseReference, filteredDraft, respondPossessionClaim);

        log.info("Draft seeded for case {} and event {} from defendant data in database",
            caseReference, respondPossessionClaim);

        return PCSCase.builder()
            .possessionClaimResponse(initialResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();
    }

    public void save(long caseReference, PCSCase caseData) {
        PCSCase draftToSave = PCSCase.builder()
            .possessionClaimResponse(caseData.getPossessionClaimResponse())
            .submitDraftAnswers(caseData.getSubmitDraftAnswers())
            .build();

        draftCaseDataService.patchUnsubmittedEventData(caseReference, draftToSave, respondPossessionClaim);

        log.debug("Draft saved successfully for case {}", caseReference);
    }
}
