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
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class LegalRepStartEventStrategy implements RespondPossessionClaimStartEventStrategy {

    private final PcsCaseService pcsCaseService;
    private final LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    private final LegalRepPartySelectionService legalRepPartySelectionService;
    private final OrganisationService organisationService;

    @Override
    public boolean supports(List<String> roles) {
        return !roles.contains(UserRole.CITIZEN.getRole());
    }

    @Override
    public PCSCase loadDraft(long caseReference, PCSCase pcsCase) {
        String organisationId = organisationService.getOrganisationIdForCurrentUser();
        // return defendants who have submitted
        if (this.legalRepPartySelectionService.hasSubmittedResponseForCurrentlySelectedParty(caseReference)) {
            List<PartyEntity> defendantPartiesLinkedAndActive = this.loadAndValidateDefendants(caseReference,
                                                                                               organisationId, false);
            return legalRepPartySelectionService.buildSubmittedResponseCase(pcsCase, defendantPartiesLinkedAndActive);
        }


        // return drafts that have not been submitted
        List<PartyEntity> defendantPartiesLinkedAndActive = this.loadAndValidateDefendants(caseReference,
                                                                                           organisationId, true);
        if (defendantPartiesLinkedAndActive.size() == 1) {
            PartyEntity defendant = defendantPartiesLinkedAndActive.getFirst();
            return legalRepPartySelectionService.getDraftCaseData(caseReference, pcsCase, defendant,
                                                                  defendantPartiesLinkedAndActive,
                                                                  organisationId);
        }

        return legalRepPartySelectionService.getDraft(pcsCase, defendantPartiesLinkedAndActive,
                                                      caseReference, organisationId);
    }

    /**
     * Returns defendant parties that do not have a submitted response for the organisation of the current user.
     *
     * @param caseReference the case reference
     * @param validate if true and there are no defendants then an exception will be thrown,
     *           if false no exception will be thrown
     * @return the list of defendant parties
     */
    private List<PartyEntity> loadAndValidateDefendants(long caseReference, String organisationId, boolean validate) {
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        return legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity,
                                                                            organisationId, validate);
    }

}
