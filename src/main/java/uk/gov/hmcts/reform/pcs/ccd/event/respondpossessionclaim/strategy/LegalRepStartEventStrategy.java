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
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
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
    private final TenancyLicenceView tenancyLicenceView;
    private final NoticeOfPossessionView noticeOfPossessionView;
    private final RentArrearsView rentArrearsView;

    @Override
    public boolean supports(List<String> roles) {
        return !roles.contains(UserRole.CITIZEN.getRole());
    }

    @Override
    public PCSCase loadDraft(long caseReference, PCSCase pcsCase) {
        String organisationId = organisationService.getOrganisationIdForCurrentUser();
        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);

        List<PartyEntity> representedDefendants =
            legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, organisationId);
        List<PartyEntity> defendantsAwaitingResponse =
            legalRepPartySelectionService.filterDefendantsAwaitingResponse(caseReference, representedDefendants);

        PCSCase responseCase;

        if (defendantsAwaitingResponse.isEmpty()
            || legalRepPartySelectionService.hasSubmittedResponseForCurrentlySelectedParty(caseReference)) {
            responseCase = legalRepPartySelectionService.buildSubmittedResponseCase(
                pcsCase, representedDefendants);
        } else if (defendantsAwaitingResponse.size() == 1) {
            PartyEntity defendant = defendantsAwaitingResponse.getFirst();
            responseCase = legalRepPartySelectionService.getDraftCaseData(
                caseReference, pcsCase, defendant, defendantsAwaitingResponse, organisationId);
        } else {
            responseCase = legalRepPartySelectionService.getDraft(
                pcsCase, defendantsAwaitingResponse, caseReference, organisationId);
        }

        return hydrateClaimantProvidedCaseFields(caseEntity, responseCase);
    }

    private PCSCase hydrateClaimantProvidedCaseFields(PcsCaseEntity caseEntity, PCSCase pcsCase) {
        tenancyLicenceView.setCaseFields(pcsCase, caseEntity);
        noticeOfPossessionView.setCaseFields(pcsCase, caseEntity);
        rentArrearsView.setCaseFields(pcsCase, caseEntity);
        pcsCase.setLegislativeCountry(caseEntity.getLegislativeCountry());
        return pcsCase;
    }

}
