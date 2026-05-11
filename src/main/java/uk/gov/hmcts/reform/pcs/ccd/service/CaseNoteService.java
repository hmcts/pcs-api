package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CaseNoteService {

    private final PcsCaseService pcsCaseService;
    private final ClaimRepository claimRepository;
    private final SecurityContextService securityContextService;
    private final Clock ukClock;

    public void addCaseNote(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        ClaimEntity claimEntity = pcsCaseEntity.getClaims().getFirst();
        CaseNoteEntity caseNoteEntity = createCaseNoteEntity(pcsCase);
        claimEntity.addCaseNote(caseNoteEntity);
        claimRepository.save(claimEntity);
    }

    private CaseNoteEntity createCaseNoteEntity(PCSCase pcsCase) {
        UserInfo userInfo = securityContextService.getCurrentUserDetails();

        return CaseNoteEntity
            .builder()
            .createdBy(userInfo.getName())
            .note(pcsCase.getNote())
            .createdOn(LocalDateTime.now(ukClock))
            .build();
    }
}
