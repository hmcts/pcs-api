package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.UUID;

/**
 * Records claim-form generation outcomes in the claim_activity_log table. Success rows carry the document
 * that was created; failure rows carry {@link GenerationDetails} in the {@code details} column.
 *
 * <p>Success is written in the caller's transaction, so it commits atomically with the document
 * attach. Failure is written in its own transaction ({@code REQUIRES_NEW}) so the row survives the
 * rollback of the generation transaction. Both methods live on this separate bean so the proxy
 * applies the propagation; a method self-invoked within the generation service would bypass it.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimActivityLogService {

    private final PcsCaseService pcsCaseService;
    private final ClaimActivityLogRepository claimActivityLogRepository;
    private final PartyRepository partyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logGenerationSuccess(long caseReference, DocumentEntity document) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        record(pcsCase, claimantParty(pcsCase), document, ClaimActivityStatus.SUCCESS, null);
    }

    @Transactional
    public void logGenerationSuccess(PcsCaseEntity pcsCase, PartyEntity party, DocumentEntity document) {
        record(pcsCase, party, document, ClaimActivityStatus.SUCCESS, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logGenerationFailure(long caseReference, GenerationDetails details) {
        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        record(pcsCase, claimantParty(pcsCase), null, ClaimActivityStatus.FAILURE, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logGenerationFailure(long caseReference, UUID partyId, GenerationDetails details) {
        PartyEntity party = partyId == null ? null : partyRepository.getReferenceById(partyId);
        record(pcsCaseService.loadCase(caseReference), party, null, ClaimActivityStatus.FAILURE, details);
    }

    private void record(PcsCaseEntity pcsCase, PartyEntity party, DocumentEntity document,
                        ClaimActivityStatus status, GenerationDetails details) {
        claimActivityLogRepository.save(
            ClaimActivityLogEntity.builder()
                .pcsCase(pcsCase)
                .party(party)
                .document(document)
                .activityType(ClaimActivityType.DOCUMENTS_CREATED)
                .status(status)
                .details(toJson(details))
                .build());
    }

    private String toJson(GenerationDetails details) {
        if (details == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            // Never fail the activity write over its detail payload; the row itself is the important record.
            log.error("Failed to serialise generation details {}", details, e);
            return null;
        }
    }

    private static PartyEntity claimantParty(PcsCaseEntity pcsCase) {
        if (pcsCase.getClaims().isEmpty()) {
            return null;
        }
        return pcsCase.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .orElse(null);
    }
}
