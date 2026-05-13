package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
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
public class CitizenStartEventStrategy extends AbstractRespondPossessionClaimStartEventStrategy {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;
    private final DefendantAccessValidator accessValidator;

    public CitizenStartEventStrategy(PossessionClaimResponseMapper responseMapper,
                                     DraftCaseDataService draftCaseDataService,
                                     PcsCaseService pcsCaseService,
                                     SecurityContextService securityContextService,
                                     DefendantAccessValidator accessValidator) {
        super(responseMapper, draftCaseDataService);
        this.pcsCaseService = pcsCaseService;
        this.securityContextService = securityContextService;
        this.accessValidator = accessValidator;
    }

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
            .possessionClaimResponse(createDefendantOnlyDraft(response))
            .build();

        draftCaseDataService.patchUnsubmittedEventData(caseReference, draft, respondPossessionClaim);

        return pcsCase.toBuilder()
            .possessionClaimResponse(response)
            .build();
    }

    private PCSCase restoreDraft(long caseReference, PCSCase pcsCase, PartyEntity defendant) {
        PCSCase savedDraft = draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .orElseThrow(() -> new DraftNotFoundException(caseReference, respondPossessionClaim));

        PossessionClaimResponse merged = mergeLatestCaseData(pcsCase, savedDraft.getPossessionClaimResponse())
            .toBuilder()
            .claimantEnteredDefendantDetails(responseMapper.buildPartyFromEntity(defendant, pcsCase))
            .build();

        return buildCaseWithDraft(pcsCase, merged);
    }
}
