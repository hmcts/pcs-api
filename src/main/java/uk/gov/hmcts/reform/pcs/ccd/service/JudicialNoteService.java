package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.time.Clock;
import java.time.Instant;

@Service
@AllArgsConstructor
public class JudicialNoteService {

    private final PcsCaseService pcsCaseService;
    private final PcsCaseRepository pcsCaseRepository;
    private final UserNameService userNameService;
    private final Clock utcClock;

    public void addJudicialNote(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        UserNameEntity userNameEntity = userNameService.getOrCreateUserNameEntity();
        JudicialNoteEntity judicialNoteEntity = createJudicialNoteEntity(pcsCase);
        pcsCaseEntity.addJudicialNote(judicialNoteEntity);
        userNameEntity.addJudicialNote(judicialNoteEntity);
        pcsCaseRepository.save(pcsCaseEntity);
    }

    private JudicialNoteEntity createJudicialNoteEntity(PCSCase pcsCase) {
        return JudicialNoteEntity.builder()
            .note(pcsCase.getJudicialNote())
            .createdOn(Instant.now(utcClock))
            .build();
    }
}
