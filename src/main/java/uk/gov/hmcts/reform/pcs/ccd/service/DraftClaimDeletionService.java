package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftClaimDeletionRepository;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DraftClaimDeletionService {

    private final DraftClaimDeletionRepository draftClaimDeletionRepository;

    @Transactional
    public void deleteDraftClaim(long caseReference) {
        UUID caseId = draftClaimDeletionRepository.getCaseId(caseReference);

        if (caseId == null) {
            draftClaimDeletionRepository.deleteDraftCaseData(caseReference);
            draftClaimDeletionRepository.deleteCcdCaseData(caseReference);
            return;
        }

        draftClaimDeletionRepository.lockClaimsForCase(caseId);

        if (draftClaimDeletionRepository.hasIssuedClaim(caseId)) {
            throw new IllegalStateException("Cannot delete a claim that has been issued");
        }

        draftClaimDeletionRepository.deleteDraftCaseData(caseReference);

        final List<UUID> addressIds = draftClaimDeletionRepository.getAddressIdsForCase(caseId);
        final List<UUID> contactPreferenceIds = draftClaimDeletionRepository.getContactPreferenceIdsForCase(caseId);
        final List<UUID> helpWithFeesIds = draftClaimDeletionRepository.getHelpWithFeesIdsForCase(caseId);
        final List<UUID> legalRepresentativeAddressIds =
            draftClaimDeletionRepository.getLegalRepresentativeAddressIdsForCase(caseId);

        draftClaimDeletionRepository.deleteRowsLinkedToCase(caseId);
        draftClaimDeletionRepository.deleteRowsLinkedToClaims(caseId);
        draftClaimDeletionRepository.deleteRowsLinkedToParties(caseId);

        draftClaimDeletionRepository.deleteCaseDocuments(caseId);
        draftClaimDeletionRepository.deleteClaims(caseId);
        draftClaimDeletionRepository.deleteParties(caseId);
        draftClaimDeletionRepository.deleteCase(caseId);

        draftClaimDeletionRepository.deleteContactPreferences(contactPreferenceIds);
        draftClaimDeletionRepository.deleteHelpWithFees(helpWithFeesIds);
        draftClaimDeletionRepository.deleteAddresses(legalRepresentativeAddressIds);
        draftClaimDeletionRepository.deleteAddresses(addressIds);

        draftClaimDeletionRepository.deleteCcdCaseData(caseReference);
    }
}
