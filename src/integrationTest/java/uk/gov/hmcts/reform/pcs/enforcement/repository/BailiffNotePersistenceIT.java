package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Bailiff note persistence (V039)")
class BailiffNotePersistenceIT extends AbstractPostgresContainerIT {

    private static final Instant FIRST_FEB = Instant.parse("2026-02-01T16:20:00Z");
    private static final Instant THIRD_FEB = Instant.parse("2026-02-03T09:00:00Z");

    @Autowired
    private BailiffNoteRepository bailiffNoteRepository;

    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;

    @Autowired
    private StatementOfTruthRepository statementOfTruthRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("stores a note with its author and time against the enforcement")
    void storesNoteWithAuthorAndTime() {
        EnforcementOrderEntity enforcement = savedEnforcement(1111222233334444L);
        String note = "Dog on site * (large)";

        UUID id = saveNote(enforcement, note, FIRST_FEB, "Sam", "Jones");

        BailiffNoteEntity reloaded = bailiffNoteRepository.findById(id).orElseThrow();
        assertThat(reloaded.getNote()).isEqualTo(note);
        assertThat(reloaded.getCreatedAt()).isEqualTo(FIRST_FEB);
        assertThat(reloaded.getAuthorFirstName()).isEqualTo("Sam");
        assertThat(reloaded.getAuthorLastName()).isEqualTo("Jones");
        assertThat(reloaded.getEnforcementOrder().getId()).isEqualTo(enforcement.getId());
        assertThat(jdbcTemplate.queryForObject(
            "SELECT enf_case_id FROM enf_bailiff_note WHERE id = ?", UUID.class, id)).isEqualTo(enforcement.getId());
    }

    @Test
    @DisplayName("lists an enforcement's notes newest first, and only that enforcement's")
    void listsNotesNewestFirstPerEnforcement() {
        EnforcementOrderEntity closed = savedEnforcement(2222333344445555L, EnforcementStatus.CLOSED);
        EnforcementOrderEntity live = savedEnforcement(2222333344445555L);
        UUID older = saveNote(live, "older", FIRST_FEB, "Sam", "Jones");
        UUID newer = saveNote(live, "newer", THIRD_FEB, "Alex", "Smith");
        saveNote(closed, "other enforcement", THIRD_FEB.plus(1, ChronoUnit.DAYS), "Sam", "Jones");

        assertThat(bailiffNoteRepository.findByEnforcementOrderIdOrderByCreatedAtDesc(live.getId()))
            .extracting(BailiffNoteEntity::getId)
            .containsExactly(newer, older);
    }

    @Test
    @DisplayName("accepts a note of 6,800 characters and rejects a longer one")
    void enforcesTheNoteLength() {
        EnforcementOrderEntity enforcement = savedEnforcement(3333444455556666L);

        UUID id = saveNote(enforcement, "a".repeat(6800), FIRST_FEB, "Sam", "Jones");
        assertThat(bailiffNoteRepository.findById(id).orElseThrow().getNote()).hasSize(6800);

        assertThatThrownBy(() -> insertNote(enforcement.getId(), "a".repeat(6801)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("requires the note to belong to an existing enforcement")
    void requiresAnExistingEnforcement() {
        assertThatThrownBy(() -> insertNote(UUID.randomUUID(), "note"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("requires the note to belong to an enforcement")
    void requiresAnEnforcement() {
        assertThatThrownBy(() -> insertNote(null, "note"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("removes the notes with their enforcement")
    void removesNotesWithTheirEnforcement() {
        EnforcementOrderEntity enforcement = savedEnforcement(4444555566667777L);
        UUID id = saveNote(enforcement, "note", FIRST_FEB, "Sam", "Jones");

        jdbcTemplate.update("DELETE FROM enf_case WHERE id = ?", enforcement.getId());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM enf_bailiff_note WHERE id = ?", Integer.class, id)).isZero();
    }

    private void insertNote(UUID enforcementId, String note) {
        jdbcTemplate.update(
            "INSERT INTO enf_bailiff_note (id, enf_case_id, note, created_at) VALUES (?, ?, ?, now())",
            UUID.randomUUID(), enforcementId, note);
    }

    private UUID saveNote(EnforcementOrderEntity enforcement, String note, Instant createdAt,
                          String firstName, String lastName) {
        UUID id = bailiffNoteRepository.save(BailiffNoteEntity.builder()
                                                 .enforcementOrder(enforcement)
                                                 .note(note)
                                                 .createdAt(createdAt)
                                                 .authorFirstName(firstName)
                                                 .authorLastName(lastName)
                                                 .build()).getId();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    private EnforcementOrderEntity savedEnforcement(long caseReference) {
        return savedEnforcement(caseReference, EnforcementStatus.AWAITING_SCHEDULING);
    }

    private EnforcementOrderEntity savedEnforcement(long caseReference, EnforcementStatus status) {
        ClaimEntity claim = pcsCaseRepository.findByCaseReference(caseReference)
            .map(pcsCase -> pcsCase.getClaims().getFirst())
            .orElseGet(() -> savedClaim(caseReference));

        StatementOfTruthEntity statementOfTruth = new StatementOfTruthEntity();
        statementOfTruth.setAccepted(YesOrNo.YES);
        statementOfTruth.setFullName("Test Name");

        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setClaim(claim);
        enforcement.setEnforcementOrder(EnforcementOrder.builder().build());
        enforcement.setStatementOfTruth(statementOfTruthRepository.save(statementOfTruth));
        enforcement.setStatus(status);
        enforcement.setStatusUpdatedAt(Instant.now());
        EnforcementOrderEntity saved = enforcementOrderRepository.save(enforcement);
        entityManager.flush();
        return saved;
    }

    private ClaimEntity savedClaim(long caseReference) {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(caseReference).build();
        ClaimEntity claim = new ClaimEntity();
        pcsCase.addClaim(claim);
        return pcsCaseRepository.save(pcsCase).getClaims().getFirst();
    }
}
