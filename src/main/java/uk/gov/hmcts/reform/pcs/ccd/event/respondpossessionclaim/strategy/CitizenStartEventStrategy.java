package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.DefendantOnlyDraftBuilder;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.PossessionClaimDraftBuilder;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.PossessionClaimMerger;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.DraftNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
@Slf4j
@AllArgsConstructor
public class CitizenStartEventStrategy implements RespondPossessionClaimStartEventStrategy {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final DefendantAccessValidator accessValidator;
    private final PossessionClaimResponseMapper responseMapper;
    private final DraftCaseDataService draftCaseDataService;
    private final PossessionClaimMerger possessionClaimMerger;
    private final PossessionClaimDraftBuilder possessionClaimDraftBuilder;
    private final DefendantOnlyDraftBuilder defendantOnlyDraftBuilder;

    @Override
    public boolean supports(List<String> roles) {
        return roles.contains(UserRole.CITIZEN.getRole());
    }

    @Override
    public PCSCase loadDraft(long caseReference, PCSCase pcsCase) {
        PartyEntity defendant = loadAndValidateDefendant(caseReference);
        if (draftCaseDataService.hasUnsubmittedCaseData(caseReference, respondPossessionClaim)) {
            return restoreDraft(caseReference, pcsCase, defendant);
        }
        return initialiseDraft(caseReference, pcsCase, defendant);
    }

    private PartyEntity loadAndValidateDefendant(long caseReference) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        return accessValidator.validateAndGetDefendant(caseEntity, securityContextService.getCurrentUserId());
    }

    private PCSCase initialiseDraft(long caseReference, PCSCase pcsCase, PartyEntity defendant) {
        PossessionClaimResponse response = responseMapper.mapFrom(pcsCase, defendant);
        PCSCase draft = PCSCase.builder()
            .possessionClaimResponse(defendantOnlyDraftBuilder.createDefendantOnlyDraft(response))
            .build();

        draftCaseDataService.patchUnsubmittedEventData(caseReference, draft, respondPossessionClaim);

        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .build();
    }

    private PCSCase restoreDraft(long caseReference, PCSCase pcsCase, PartyEntity defendant) {
        PCSCase savedDraft = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        PossessionClaimResponse merged = possessionClaimMerger.mergeLatestCaseData(pcsCase,
                                                                                   savedDraft
                                                                                       .getPossessionClaimResponse())
            .toBuilder()
            .claimantEnteredDefendantDetails(responseMapper.buildPartyFromEntity(defendant, pcsCase))
            .build();

        return possessionClaimDraftBuilder.buildCaseWithDraft(pcsCase, merged);
    }
}
