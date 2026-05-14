package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class LegalRepresentativeCaseDraftLoader {

    private final PcsCaseService pcsCaseService;
    private final LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    private final SecurityContextService securityContextService;
    private final LegalRepPartySelectionService legalRepPartySelectionService;

    public PCSCase loadDraft(long caseReference, PCSCase pcsCase) {
        List<PartyEntity> defendantPartiesLinkedAndActive = loadAndValidateDefendantsForLegalRep(caseReference);

        if (defendantPartiesLinkedAndActive.size() == 1) {
            return legalRepPartySelectionService.getDraftCaseData(caseReference, pcsCase,
                                                                  defendantPartiesLinkedAndActive.getFirst(), true);
        }
        return legalRepPartySelectionService.getDraft(pcsCase, defendantPartiesLinkedAndActive, caseReference);
    }

    private List<PartyEntity> loadAndValidateDefendantsForLegalRep(long caseReference) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        return legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity,
            securityContextService.getCurrentUserId());
    }

}
