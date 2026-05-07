package uk.gov.hmcts.reform.pcs.ccd.service.party;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.util.List;
import java.util.UUID;

/**
 * Validates that the authenticated user has defendant access to a case.
 *
 * <p>Ensures that:
 * 1. The case has at least one claim
 * 2. The claim has at least one defendant
 * 3. The user is linked as a defendant on the case
 */
@Service
@Slf4j
public class DefendantAccessValidator {

    /**
     * Validates that the authenticated user has defendant access and returns the matched defendant.
     *
     * @param caseEntity The case entity (already loaded from database)
     * @param authenticatedUserId The authenticated user's IDAM ID
     * @return The matched PartyEntity representing the defendant
     * @throws CaseAccessException if the user doesn't have defendant access
     */
    public PartyEntity validateAndGetDefendant(PcsCaseEntity caseEntity, UUID authenticatedUserId) {
        long caseReference = caseEntity.getCaseReference();
        List<PartyEntity> defendants = extractDefendants(caseEntity, caseReference);
        return findMatchingDefendant(defendants, authenticatedUserId, caseReference);
    }

    private List<PartyEntity> extractDefendants(PcsCaseEntity caseEntity, long caseReference) {
        ClaimEntity mainClaim = caseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> {
                log.error("No claim found for case {}", caseReference);
                return new CaseAccessException("No claim found for this case");
            });

        List<PartyEntity> defendants = mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();

        if (defendants.isEmpty()) {
            log.error("No defendants found for case {}", caseReference);
            throw new CaseAccessException("No defendants associated with this case");
        }

        return defendants;
    }

    private PartyEntity findMatchingDefendant(
        List<PartyEntity> defendants,
        UUID authenticatedUserId,
        long caseReference
    ) {
        return defendants.stream()
            .filter(defendant -> authenticatedUserId.equals(defendant.getIdamId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error(
                    "Access denied: User {} is not linked as a defendant on case {}",
                    authenticatedUserId,
                    caseReference
                );
                return new CaseAccessException("User is not linked as a defendant on this case");
            });
    }
}
