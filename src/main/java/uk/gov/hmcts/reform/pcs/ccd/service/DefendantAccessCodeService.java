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
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.PinPackDocumentGenerator;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;
import uk.gov.hmcts.reform.pcs.testingsupport.service.TestPinRecorder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates a single defendant's access code and pin pack in its own transaction, so that a failure
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
    private final PinPackDocumentGenerator pinPackDocumentGenerator;
    private final DocumentRepository documentRepository;
    private final ClaimActivityLogService claimActivityLogService;
    private final TestPinRecorder testPinRecorder;

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
        ClaimEntity mainClaim = getMainClaim(pcsCaseEntity);
        PartyEntity defendant = findDefendant(mainClaim, defendantPartyId);

        try {
            String plaintextAccessCode = accessCodeGenerator.generateAccessCode();

            String documentUrl = pinPackDocumentGenerator
                .generatePinPack(pcsCaseEntity, mainClaim, defendant, plaintextAccessCode);

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

            testPinRecorder.record(pcsCaseEntity.getId(), defendant.getId(), plaintextAccessCode);

            claimActivityLogService.logSuccess(pcsCaseEntity, defendant, ClaimActivityType.DOCUMENTS_CREATED);

        } catch (Exception e) {
            log.error("Failed to generate pin pack / access code for party {} on case {}",
                      defendant.getId(), pcsCaseEntity.getCaseReference(), e);
            if (finalAttempt) {
                claimActivityLogService.logFailure(pcsCaseEntity, defendant, ClaimActivityType.DOCUMENTS_CREATED);
            }
            throw e;
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
