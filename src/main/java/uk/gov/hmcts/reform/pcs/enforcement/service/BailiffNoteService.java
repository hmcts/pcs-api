package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffNoteRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BailiffNoteService {

    private final CurrentEnforcementService currentEnforcementService;
    private final BailiffNoteRepository bailiffNoteRepository;
    private final SecurityContextService securityContextService;
    private final Clock utcClock;

    public static boolean canAddBailiffNote(EnforcementOrderEntity currentEnforcement) {
        return currentEnforcement != null;
    }

    public boolean hasEnforcement(long caseReference) {
        return canAddBailiffNote(currentEnforcementService.findCurrentEnforcement(caseReference).orElse(null));
    }

    @Transactional
    public String addNote(long caseReference, String note) {
        EnforcementOrderEntity enforcement = currentEnforcementService.findCurrentEnforcement(caseReference)
            .orElseThrow(NoEnforcementException::new);
        UserInfo author = securityContextService.getCurrentUserDetails();

        bailiffNoteRepository.save(BailiffNoteEntity.builder()
                                       .enforcementOrder(enforcement)
                                       .note(note)
                                       .createdAt(Instant.now(utcClock))
                                       .authorFirstName(author.getGivenName())
                                       .authorLastName(author.getFamilyName())
                                       .build());
        return enforcement.getEnforcementNumber();
    }

    public List<BailiffNoteEntity> getCurrentEnforcementNotes(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)
            .map(enforcement -> bailiffNoteRepository.findByEnforcementOrderIdOrderByCreatedAtDesc(enforcement.getId()))
            .orElse(List.of());
    }

    public List<BailiffNoteEntity> getNotes(EnforcementOrderEntity enforcement) {
        return bailiffNoteRepository.findByEnforcementOrderIdOrderByCreatedAtDesc(enforcement.getId());
    }
}
