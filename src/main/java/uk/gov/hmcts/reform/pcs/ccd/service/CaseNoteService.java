package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
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

    public void addCaseNote(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        CaseNoteEntity caseNoteEntity = createCaseNoteEntity(pcsCase);
        pcsCaseEntity.addCaseNote(caseNoteEntity);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    private CaseNoteEntity createCaseNoteEntity(PCSCase pcsCase) {
        UserInfo userInfo = securityContextService.getCurrentUserDetails();

        return CaseNoteEntity
            .builder()
            .createdBy(userInfo.getName())
            .note(pcsCase.getNote())
            .createdOn(Instant.now(utcClock))
            .build();
    }
}
