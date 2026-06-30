package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeFormDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;
import uk.gov.hmcts.reform.pcs.testingsupport.service.TestAccessCodeRecorder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates a single defendant's access code letter in its own transaction, so that a failure
 * for one defendant does not roll back the others on a 1-v-many case. Failure is recorded to
 * {@code claim_activity_log} only on the final scheduler attempt to avoid a row per retry.
 */
@Service
@AllArgsConstructor
@Slf4j
public class DefendantAccessCodeService {

    private final PcsCaseService pcsCaseService;
    private final PartyAccessCodeRepository partyAccessCodeRepo;
    private final AccessCodeGenerator accessCodeGenerator;
    private final PartyAccessCodeHashingService hashingService;
    private final AccessCodeFormDocumentGenerator accessCodeFormDocumentGenerator;
    private final DocumentRepository documentRepository;
    private final AccessCodeActivityLogService accessCodeActivityLogService;
    private final TestAccessCodeRecorder testAccessCodeRecorder;

    @Transactional(readOnly = true)
    public List<UUID> findDefendantPartyIdsNeedingAccessCode(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        Set<UUID> partyIdsWithCode = partyAccessCodeRepo.findAllByPcsCase_Id(pcsCaseEntity.getId()).stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .collect(Collectors.toSet());

        return mainClaimDefendants(getMainClaim(pcsCaseEntity)).stream()
            .map(PartyEntity::getId)
            .filter(partyId -> !partyIdsWithCode.contains(partyId))
            .toList();
    }

    @Transactional
    public void generateForDefendant(long caseReference, UUID defendantPartyId, boolean finalAttempt) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        // Idempotent: a re-fired payment or a scheduler retry must not mint a second code for a party
        // that already has one. The UNIQUE(case_id, party_id) constraint is the final backstop.
        if (partyAccessCodeRepo.existsByPcsCase_IdAndPartyId(pcsCaseEntity.getId(), defendantPartyId)) {
            log.info("Access code already exists for party {} on case {} - skipping", defendantPartyId, caseReference);
            return;
        }

        ClaimEntity mainClaim = getMainClaim(pcsCaseEntity);
        PartyEntity defendant = findDefendant(mainClaim, defendantPartyId);

        try {
            String plaintextAccessCode = accessCodeGenerator.generateAccessCode();

            String documentUrl = accessCodeFormDocumentGenerator
                .generate(pcsCaseEntity, mainClaim, defendant, plaintextAccessCode);

            documentRepository.save(
                DocumentEntity.builder()
                    .pcsCase(pcsCaseEntity)
                    .party(defendant)
                    .url(documentUrl)
                    .type(DocumentType.DEFENDANT_ACCESS_CODE)
                    .build()
            );

            partyAccessCodeRepo.save(
                PartyAccessCodeEntity.builder()
                    .partyId(defendant.getId())
                    .pcsCase(pcsCaseEntity)
                    .code(hashingService.encodeForStorage(plaintextAccessCode))
                    .role(PartyRole.DEFENDANT)
                    .build()
            );

            testAccessCodeRecorder.record(pcsCaseEntity.getId(), defendant.getId(), plaintextAccessCode);

            accessCodeActivityLogService.logSuccess(pcsCaseEntity, defendant, ClaimActivityType.DOCUMENTS_CREATED);

        } catch (Exception e) {
            // Terminal-only: intermediate retries are tracked in scheduled_tasks; mirror the claim-form
            // task's logging so both pipelines behave the same.
            if (finalAttempt) {
                log.error("Access-code letter generation permanently failed for party {} on case {}",
                          defendant.getId(), pcsCaseEntity.getCaseReference(), e);
                recordFailureSafely(pcsCaseEntity, defendant);
            }
            throw e;
        }
    }

    private void recordFailureSafely(PcsCaseEntity pcsCaseEntity, PartyEntity defendant) {
        try {
            accessCodeActivityLogService.logFailure(pcsCaseEntity, defendant, ClaimActivityType.DOCUMENTS_CREATED);
        } catch (Exception e) {
            log.error("Failed to record access-code FAILURE for party {} on case {}",
                      defendant.getId(), pcsCaseEntity.getCaseReference(), e);
        }
    }

    private static PartyEntity findDefendant(ClaimEntity mainClaim, UUID defendantPartyId) {
        return mainClaimDefendants(mainClaim).stream()
            .filter(party -> defendantPartyId.equals(party.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Defendant " + defendantPartyId + " not found on main claim"));
    }

    private static List<PartyEntity> mainClaimDefendants(ClaimEntity mainClaim) {
        return mainClaim.getClaimParties().stream()
            .filter(claimParty -> PartyRole.DEFENDANT == claimParty.getRole())
            .map(ClaimPartyEntity::getParty)
            .toList();
    }

    private static ClaimEntity getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> new ClaimNotFoundException(pcsCaseEntity.getCaseReference()));
    }
}
