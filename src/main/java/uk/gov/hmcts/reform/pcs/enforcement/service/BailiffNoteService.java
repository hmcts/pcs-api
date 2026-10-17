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

    public boolean hasEnforcement(long caseReference) {
        return currentEnforcementService.findCurrentEnforcement(caseReference).isPresent();
    }

    /**
     * Saves the note against the claim's current enforcement, with the signed-in user's IDAM name.
     *
     * @throws NoEnforcementException if the claim has no enforcement
     */
    @Transactional
    public void addNote(long caseReference, String note) {
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
    }

    /**
     * The notes on the claim's current enforcement, newest first. Empty if the claim has no enforcement.
     */
    public List<BailiffNoteEntity> getCurrentEnforcementNotes(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)
            .map(enforcement -> bailiffNoteRepository.findByEnforcementOrderIdOrderByCreatedAtDesc(enforcement.getId()))
            .orElse(List.of());
    }
}
