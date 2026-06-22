package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates defendant access code / pin pack generation for a case. Each defendant is generated
 * in its own transaction via {@link DefendantAccessCodeService}, so one defendant's failure does not
 * roll back the others; defendants that already have a code are skipped, making retries idempotent.
 * If any defendant fails, the whole task is failed so the scheduler retries the remaining ones.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeGenerationService {

    private final DefendantAccessCodeService defendantAccessCodeService;

    public void createAccessCodesForParties(String caseReference, boolean finalAttempt) {
        long caseReferenceNumber = Long.parseLong(caseReference);

        List<UUID> defendantPartyIds =
            defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(caseReferenceNumber);

        List<UUID> failedDefendantPartyIds = new ArrayList<>();
        for (UUID defendantPartyId : defendantPartyIds) {
            try {
                defendantAccessCodeService.generateForDefendant(caseReferenceNumber, defendantPartyId, finalAttempt);
            } catch (Exception e) {
                failedDefendantPartyIds.add(defendantPartyId);
            }
        }

        if (!failedDefendantPartyIds.isEmpty()) {
            throw new IllegalStateException("Access code generation failed for defendants "
                + failedDefendantPartyIds + " on case " + caseReference);
        }

        if (!defendantPartyIds.isEmpty()) {
            log.debug("Generated {} defendant access code pin pack(s) for case {}",
                      defendantPartyIds.size(), caseReference);
        }
    }
}
