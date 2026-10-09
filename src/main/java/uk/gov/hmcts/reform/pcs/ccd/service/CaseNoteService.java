package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseNoteRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;

@Service
@AllArgsConstructor
public class CaseNoteService {

    private final PcsCaseService pcsCaseService;
    private PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final Clock utcClock;
    private final CaseNoteRepository caseNoteRepository;

    public void addCaseNote(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        CaseNoteEntity caseNoteEntity = createCaseNoteEntity(pcsCase);
        pcsCaseEntity.addCaseNote(caseNoteEntity);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    /** Adds a note to the case as the current user, saved so that other records can point at it. */
    public CaseNoteEntity addCaseNote(PcsCaseEntity pcsCaseEntity, String note) {
        CaseNoteEntity caseNoteEntity = createCaseNoteEntity(note);
        pcsCaseEntity.addCaseNote(caseNoteEntity);
        return caseNoteRepository.save(caseNoteEntity);
    }

    private CaseNoteEntity createCaseNoteEntity(PCSCase pcsCase) {
        return createCaseNoteEntity(pcsCase.getNote());
    }

    private CaseNoteEntity createCaseNoteEntity(String note) {
        UserInfo userInfo = securityContextService.getCurrentUserDetails();

        return CaseNoteEntity
            .builder()
            .createdBy(userInfo.getName())
            .note(note)
            .createdOn(Instant.now(utcClock))
            .build();
    }
}
