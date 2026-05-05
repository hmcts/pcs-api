package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@AllArgsConstructor
public class CaseNoteService {

    private PcsCaseService pcsCaseService;
    private ClaimRepository claimRepository;

    public CaseNoteEntity createCaseNote(PCSCase pcsCase) {
        CaseNoteEntity entity = new CaseNoteEntity();
        entity.setCreatedBy("test-user");
        entity.setNote(pcsCase.getNote());
        entity.setCreatedOn(LocalDateTime.now());

        return entity;
    }

    public void addCaseNote(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        ClaimEntity claimEntity = pcsCaseEntity.getClaims().getFirst();
        CaseNoteEntity caseNoteEntity = createCaseNote(pcsCase);
        caseNoteEntity.setClaim(claimEntity);
        if (claimEntity.getCaseNotes() == null) {
            claimEntity.setCaseNotes(new ArrayList<>());
        }
        claimEntity.getCaseNotes().add(caseNoteEntity);
        claimRepository.save(claimEntity);
    }
}
