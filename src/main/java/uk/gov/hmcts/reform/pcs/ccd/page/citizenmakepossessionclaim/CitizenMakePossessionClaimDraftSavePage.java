package uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenCreatePossessionClaim.SERVICE_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.CASE_ACCESS_DENIED;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.DRAFT_CHANGED;

@Slf4j
@Component
@AllArgsConstructor
public class CitizenMakePossessionClaimDraftSavePage implements CcdPageConfiguration {

    public static final String PAGE_ID = "citizenMakePossessionClaimDraftSavePage";
    public static final String SAVE_FAILED = "We couldn't save your claim. Please try again or contact support.";

    private final FeatureToggleService featureToggleService;
    private final CitizenPossessionClaimCaseService citizenPossessionClaimCaseService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page(PAGE_ID, this::midEvent)
            .pageLabel("Citizen make a claim")
            .label(PAGE_ID + "-info", "Placeholder page to save draft data via mid event");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        if (!featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)) {
            return error(SERVICE_NOT_AVAILABLE);
        }

        long caseReference = details.getId();
        try {
            PcsCaseEntity pcsCaseEntity = citizenPossessionClaimCaseService.loadCase(caseReference);
            if (!citizenPossessionClaimCaseService.isCurrentUserClaimCreator(pcsCaseEntity)) {
                return error(CASE_ACCESS_DENIED);
            }

            CitizenPossessionClaim claim = Optional.ofNullable(details.getData().getCitizenPossessionClaim())
                .orElseGet(() -> CitizenPossessionClaim.builder().build());

            Long savedVersion = citizenPossessionClaimCaseService.saveDraft(caseReference, claim);

            CitizenPossessionClaim saved = CitizenPossessionClaimCaseService.withoutServerOwnedFields(claim);
            saved.setPropertyAddress(citizenPossessionClaimCaseService.caseAddress(pcsCaseEntity));
            saved.setDraftVersion(savedVersion);

            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(PCSCase.builder().citizenPossessionClaim(saved).build())
                .build();
        } catch (DraftVersionConflictException e) {
            log.warn("Rejecting citizen claim draft save for case {}: {}", caseReference, e.getMessage());
            return error(DRAFT_CHANGED);
        } catch (Exception e) {
            log.error("Failed to save citizen claim draft for case {}", caseReference, e);
            return error(SAVE_FAILED);
        }
    }

    private static AboutToStartOrSubmitResponse<PCSCase, State> error(String message) {
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errors(List.of(message))
            .build();
    }
}
