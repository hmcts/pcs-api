package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.GenerationDetails;
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
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeFormDocumentGenerator.LetterContext;
import uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.util.AccessCodeGenerator;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;
import uk.gov.hmcts.reform.pcs.testingsupport.service.TestAccessCodeRecorder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage.ENGLISH;
import static uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage.WELSH;

/**
 * Generates a single defendant's access code letter in its own transaction, so that a failure
 * for one defendant does not roll back the others on a 1-v-many case. Failure is recorded to
 * {@code claim_activity_log} only on the final scheduler attempt to avoid a row per retry.
 * A defendant on a Wales case gets an English and a Welsh letter with the same code; both are rendered
 * before anything is saved, so a failure leaves neither behind.
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
    private final DocumentIdExtractor documentIdExtractor;
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
    public void generateForDefendant(long caseReference, UUID defendantPartyId,
                                     boolean firstAttempt, boolean finalAttempt) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        // Idempotent: a re-fired payment or a scheduler retry must not mint a second code for a party
        // that already has one. The UNIQUE(case_id, party_id) constraint is the final backstop.
        if (partyAccessCodeRepo.existsByPcsCase_IdAndPartyId(pcsCaseEntity.getId(), defendantPartyId)) {
            log.info("Access code already exists for party {} on case {} - skipping", defendantPartyId, caseReference);
            return;
        }

        ClaimEntity mainClaim = getMainClaim(pcsCaseEntity);
        PartyEntity defendant = findDefendant(mainClaim, defendantPartyId);

        boolean walesCase = pcsCaseEntity.getLegislativeCountry() == LegislativeCountry.WALES;
        Attempt attempt = new Attempt(pcsCaseEntity, defendant, walesCase, firstAttempt, finalAttempt);

        final LetterContext letterContext;
        final String plaintextAccessCode;
        try {
            letterContext = accessCodeFormDocumentGenerator.resolveLetterContext(pcsCaseEntity);
            plaintextAccessCode = accessCodeGenerator.generateAccessCode();
        } catch (Exception e) {
            recordFailure(attempt, e, null);
            throw e;
        }

        // Render every letter before saving anything, so a failure leaves no document or code behind.
        String englishUrl = render(attempt, mainClaim, plaintextAccessCode, ENGLISH, letterContext);
        String welshUrl = walesCase ? render(attempt, mainClaim, plaintextAccessCode, WELSH, letterContext) : null;

        try {
            saveLetter(pcsCaseEntity, defendant, englishUrl, ENGLISH);
            if (welshUrl != null) {
                saveLetter(pcsCaseEntity, defendant, welshUrl, WELSH);
            }

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
            recordFailure(attempt, e, null);
            throw e;
        }
    }

    private String render(Attempt attempt, ClaimEntity mainClaim, String plaintextAccessCode,
                          AccessCodeLetterLanguage language, LetterContext letterContext) {
        try {
            return accessCodeFormDocumentGenerator.generate(attempt.pcsCase(), mainClaim, attempt.defendant(),
                                                            plaintextAccessCode, language, letterContext);
        } catch (Exception e) {
            // Only a Wales case has two letters, so only there does the failure name which one broke.
            recordFailure(attempt, e, attempt.walesCase() ? language.getLabel() + " letter" : null);
            throw e;
        }
    }

    // First + terminal recording: the first failure makes the reason visible immediately
    // (terminal:false = still retrying); intermediate retries stay in scheduled_tasks/logs;
    // the final attempt writes the terminal row. Mirrors the claim-form task.
    private void recordFailure(Attempt attempt, Exception cause, String failedLetter) {
        if (attempt.finalAttempt()) {
            log.error("Access-code letter generation permanently failed for party {} on case {}",
                      attempt.defendant().getId(), attempt.pcsCase().getCaseReference(), cause);
        }
        if (attempt.firstAttempt() || attempt.finalAttempt()) {
            recordFailureSafely(attempt.pcsCase(), attempt.defendant(), cause, attempt.finalAttempt(), failedLetter);
        }
    }

    private void recordFailureSafely(PcsCaseEntity pcsCaseEntity, PartyEntity defendant, Exception cause,
                                     boolean terminal, String failedLetter) {
        try {
            accessCodeActivityLogService.logFailure(pcsCaseEntity, defendant, ClaimActivityType.DOCUMENTS_CREATED,
                GenerationDetails.forFailure(DocumentType.DEFENDANT_ACCESS_CODE, cause, terminal, failedLetter));
        } catch (Exception e) {
            log.error("Failed to record access-code FAILURE for party {} on case {}",
                      defendant.getId(), pcsCaseEntity.getCaseReference(), e);
        }
    }

    private void saveLetter(PcsCaseEntity pcsCaseEntity, PartyEntity defendant, String documentUrl,
                            AccessCodeLetterLanguage language) {
        documentRepository.save(
            DocumentEntity.builder()
                .pcsCase(pcsCaseEntity)
                .party(defendant)
                .url(documentUrl)
                .documentId(documentIdExtractor.extractDocumentId(documentUrl))
                .type(DocumentType.DEFENDANT_ACCESS_CODE)
                .categoryId(CaseFileCategory.UNCATEGORISED_DOCUMENTS.getId())
                // The English letter keeps no file name, as on master; the language column tells the letters apart.
                .fileName(language == WELSH
                    ? AccessCodeFormDocumentGenerator.outputFileName(defendant.getId(), WELSH) : null)
                .language(language)
                .build()
        );
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
            .filter(defendant -> !defendant.isRemoved())
            .toList();
    }

    private static ClaimEntity getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> new ClaimNotFoundException(pcsCaseEntity.getCaseReference()));
    }

    private record Attempt(PcsCaseEntity pcsCase, PartyEntity defendant, boolean walesCase,
                           boolean firstAttempt, boolean finalAttempt) {
    }
}
