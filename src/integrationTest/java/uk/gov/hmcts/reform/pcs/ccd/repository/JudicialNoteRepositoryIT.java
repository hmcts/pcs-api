package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PcsCcdEventTest
@Transactional
class JudicialNoteRepositoryIT extends AbstractPostgresContainerIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserNameRepository userNameRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Test
    void rejectsANoteForANonexistentCase() {
        UserNameEntity judge = userNameRepository.saveAndFlush(UserNameEntity.builder()
            .idamId("judicial-note-judge")
            .name("A Judge")
            .build());

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO judicial_note (note, user_id, case_id) VALUES (?, ?, ?)",
            "A note", judge.getId(), UUID.randomUUID()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsANoteForANonexistentUser() {
        PcsCaseEntity pcsCase = pcsCaseRepository.saveAndFlush(PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .build());

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO judicial_note (note, user_id, case_id) VALUES (?, ?, ?)",
            "A note", -1, pcsCase.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
