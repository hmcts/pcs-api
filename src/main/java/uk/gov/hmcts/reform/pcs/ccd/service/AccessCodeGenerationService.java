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
 * For each new defendant on the main claim, generates an access code, renders the pin pack via
 * Docmosis, persists the document + BCrypt-encoded code, and records the result to
 * {@code claim_activity_log}. Idempotent: defendants that already have a code are skipped.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeGenerationService {

    private final PartyAccessCodeRepository partyAccessCodeRepo;
    private final PcsCaseService pcsCaseService;
    private final AccessCodeGenerator accessCodeGenerator;
    private final PartyAccessCodeHashingService hashingService;
    private final PinPackDocumentGenerator pinPackDocumentGenerator;
    private final DocumentRepository documentRepository;
    private final ClaimActivityLogService claimActivityLogService;
    private final TestPinRecorder testPinRecorder;

    @Transactional
    public void createAccessCodesForParties(String caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(Long.parseLong(caseReference));

        Set<UUID> existingPartyIds = partyAccessCodeRepo.findAllByPcsCase_Id(pcsCaseEntity.getId())
            .stream()
            .map(PartyAccessCodeEntity::getPartyId)
            .collect(Collectors.toSet());

        ClaimEntity mainClaim = getMainClaim(pcsCaseEntity);

        List<PartyEntity> newDefendants = getMainClaimDefendants(mainClaim).stream()
            .filter(defendant -> !existingPartyIds.contains(defendant.getId()))
            .toList();

        for (PartyEntity defendant : newDefendants) {
            generateAccessCodeAndPinPack(pcsCaseEntity, mainClaim, defendant);
        }

        if (!newDefendants.isEmpty()) {
            log.debug("Generated {} defendant access code pin pack(s) for case {}",
                      newDefendants.size(), caseReference);
        }
    }

    private void generateAccessCodeAndPinPack(PcsCaseEntity pcsCaseEntity,
                                              ClaimEntity mainClaim,
                                              PartyEntity defendant) {
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
            claimActivityLogService.logFailure(pcsCaseEntity, defendant, ClaimActivityType.DOCUMENTS_CREATED);
            throw e;
        }
    }

    private static List<PartyEntity> getMainClaimDefendants(ClaimEntity mainClaim) {
        return mainClaim.getClaimParties()
            .stream()
            .filter(claimPartyEntity -> PartyRole.DEFENDANT == claimPartyEntity.getRole())
            .map(ClaimPartyEntity::getParty)
            .toList();
    }

    // TODO: Will be refactored as part of HDPI-3232
    private static ClaimEntity getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims()
            .stream()
            .findFirst()
            .orElseThrow(() -> new ClaimNotFoundException(pcsCaseEntity.getCaseReference()));
    }
}
