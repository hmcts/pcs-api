package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemCaseEvent;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventOutcome;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates every defendant's access code for a case <b>synchronously</b>, in one call. Used only by
 * testing-support so a functional test can issue a case and read the PINs immediately; the production
 * (scheduled) path fans out one task per defendant instead - see {@code AccessCodeGenerationComponent}.
 * Each defendant is generated in its own transaction via {@link DefendantAccessCodeService}, so one
 * failure does not roll back the others; defendants that already have a code are skipped. If any
 * defendant fails, the whole call fails so the caller sees it.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeGenerationService {

    private final DefendantAccessCodeService defendantAccessCodeService;
    private final SystemCaseEventService systemCaseEventService;

    public void createAccessCodesForParties(String caseReference, boolean finalAttempt) {
        long caseReferenceNumber = Long.parseLong(caseReference);

        List<UUID> defendantPartyIds =
            defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(caseReferenceNumber);

        List<UUID> failedDefendantPartyIds = new ArrayList<>();
        for (UUID defendantPartyId : defendantPartyIds) {
            try {
                // synchronous single-shot: this call is its own first attempt
                systemCaseEventService.submit(
                    caseReferenceNumber,
                    new SystemCaseEvent("testAccessCodeGenerated", "Test access code generated"),
                    idempotencyKey(caseReferenceNumber, defendantPartyId),
                    context -> {
                        defendantAccessCodeService.generateForDefendant(
                            caseReferenceNumber, defendantPartyId, true, finalAttempt
                        );
                        return SystemCaseEventOutcome.noStateChange();
                    }
                );
            } catch (Exception e) {
                failedDefendantPartyIds.add(defendantPartyId);
            }
        }

        if (!failedDefendantPartyIds.isEmpty()) {
            throw new IllegalStateException("Access code generation failed for defendants "
                + failedDefendantPartyIds + " on case " + caseReference);
        }

        if (!defendantPartyIds.isEmpty()) {
            log.debug("Generated {} defendant access-code letter(s) for parties {} on case {}",
                      defendantPartyIds.size(), defendantPartyIds, caseReference);
        }
    }

    private UUID idempotencyKey(long caseReference, UUID partyId) {
        return UUID.nameUUIDFromBytes(
            ("pcs:test-access-code-generated:" + caseReference + ":" + partyId)
                .getBytes(StandardCharsets.UTF_8)
        );
    }
}
