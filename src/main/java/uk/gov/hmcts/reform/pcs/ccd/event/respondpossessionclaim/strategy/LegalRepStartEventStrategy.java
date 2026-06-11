package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.LegalRepPartySelectionService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class LegalRepStartEventStrategy implements RespondPossessionClaimStartEventStrategy {

    private final PcsCaseService pcsCaseService;
    private final LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    private final SecurityContextService securityContextService;
    private final LegalRepPartySelectionService legalRepPartySelectionService;
    private final OrganisationDetailsService organisationDetailsService;

    @Override
    public boolean supports(List<String> roles) {
        return !roles.contains(UserRole.CITIZEN.getRole());
    }

    @Override
    public PCSCase loadDraft(long caseReference, PCSCase pcsCase) {
        String organisationId = organisationDetailsService
            .getOrganisationIdentifier(securityContextService.getCurrentUserId().toString());

        List<PartyEntity> defendantPartiesLinkedAndActive = loadAndValidateDefendants(caseReference, organisationId);

        if (defendantPartiesLinkedAndActive.size() == 1) {
            PartyEntity defendant = defendantPartiesLinkedAndActive.getFirst();
            legalRepPartySelectionService.validateResponseNotAlreadySubmitted(caseReference, defendant.getId());

            return legalRepPartySelectionService.getDraftCaseData(caseReference, pcsCase, defendant,
                                                                  defendantPartiesLinkedAndActive,
                                                                  organisationId);
        }

        return legalRepPartySelectionService.getDraft(pcsCase, defendantPartiesLinkedAndActive,
                                                      caseReference, organisationId);
    }

    private List<PartyEntity> loadAndValidateDefendants(long caseReference, String organisationId) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        return legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity,
                                                                            organisationId);
    }

}
