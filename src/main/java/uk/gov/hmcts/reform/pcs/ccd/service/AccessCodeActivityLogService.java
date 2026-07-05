package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ActivityDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

/**
 * Records rows to {@code claim_activity_log}: generation outcomes ({@code DOCUMENTS_CREATED}, with
 * {@link GenerationDetails} on failure) and, for bulk print, one {@code PACK_SENT}/{@code PACK_FAILED}
 * row per (recipient, pack) carrying {@link PackDetails}. Document relationships are not stored here —
 * they live on the document table and its owning pointers.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeActivityLogService {

    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final ObjectMapper objectMapper;

    public void logSuccess(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityType activityType) {
        save(pcsCase, party, activityType, ClaimActivityStatus.SUCCESS, null);
    }

    /**
     * Uses REQUIRES_NEW so the failure row survives a rollback in the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityType activityType) {
        save(pcsCase, party, activityType, ClaimActivityStatus.FAILURE, null);
    }

    /**
     * Generation failure with {@link GenerationDetails} (which document type, why, terminal); REQUIRES_NEW
     * so the row survives the caller's rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityType activityType,
                           GenerationDetails details) {
        save(pcsCase, party, activityType, ClaimActivityStatus.FAILURE, details);
    }

    /**
     * One pack dispatch to a recipient succeeded; {@link PackDetails} records the pack type and contents
     * and is the bulk-print dedup source, so it commits in its own transaction: the post is irreversible,
     * and the send runs outside any caller transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPackSent(PcsCaseEntity pcsCase, PartyEntity recipient, PackDetails details) {
        save(pcsCase, recipient, ClaimActivityType.PACK_SENT, ClaimActivityStatus.SUCCESS, details);
    }

    /**
     * One pack dispatch to a recipient failed; {@link PackDetails} carries failureReason + terminal.
     * REQUIRES_NEW so the row survives the caller's rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPackFailed(PcsCaseEntity pcsCase, PartyEntity recipient, PackDetails details) {
        save(pcsCase, recipient, ClaimActivityType.PACK_FAILED, ClaimActivityStatus.FAILURE, details);
    }

    private void save(PcsCaseEntity pcsCase,
                      PartyEntity party,
                      ClaimActivityType activityType,
                      ClaimActivityStatus status,
                      ActivityDetails details) {

        claimActivityLogRepository.save(
            ClaimActivityLogEntity.builder()
                .pcsCase(pcsCase)
                .party(party)
                .activityType(activityType)
                .status(status)
                .details(toJson(details))
                .build()
        );

        log.debug("Recorded claim activity {} with status {} for case {}{}",
                  activityType,
                  status,
                  pcsCase.getId(),
                  party != null ? " and party " + party.getId() : "");
    }

    private String toJson(ActivityDetails details) {
        if (details == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            // Never fail the activity write over its detail payload; the row itself is the important record.
            log.error("Failed to serialise activity details {}", details, e);
            return null;
        }
    }
}
