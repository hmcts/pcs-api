package uk.gov.hmcts.reform.pcs.enforcement.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.enforcement.service.DraftEnforcementDeletionService;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The enforcement draft sweep against Postgres: a draft is discarded by when it was last saved, which Hibernate
 * keeps on draft_case_data.updated_at for every journey's drafts.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement draft deletion by last save")
class DraftEnforcementDeletionIT extends AbstractPostgresContainerIT {

    private static final int DISCARD_AFTER_DAYS = 30;

    @Autowired
    private DraftEnforcementDeletionService draftEnforcementDeletionService;
    @Autowired
    private DraftCaseDataRepository draftCaseDataRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("keeps an old draft saved recently, and deletes one left unsaved for longer than the period")
    void deletesByLastSave() {
        final int startedLongAgoSavedRecently = draft(8_100_000_000_000_001L, EventId.enforceTheOrder, 90, 1);
        final int leftUnsaved = draft(8_100_000_000_000_002L, EventId.enforceTheOrder, 40, 40);
        final int otherJourney = draft(8_100_000_000_000_003L, EventId.resumePossessionClaim, 40, 40);

        draftEnforcementDeletionService.deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);
        entityManager.clear();

        assertThat(draftCaseDataRepository.existsById(startedLongAgoSavedRecently)).isTrue();
        assertThat(draftCaseDataRepository.existsById(leftUnsaved)).isFalse();
        assertThat(draftCaseDataRepository.existsById(otherJourney)).isTrue();
    }

    @Test
    @DisplayName("every save of a draft, from any journey, moves its last save on and leaves when it was created")
    void savingADraftRecordsTheSave() {
        int id = draft(8_100_000_000_000_004L, EventId.resumePossessionClaim, 40, 40);
        final Timestamp created = column(id, "created_at");

        DraftCaseDataEntity draft = draftCaseDataRepository.findById(id).orElseThrow();
        draft.setCaseData("{\"answer\": \"changed\"}");
        draftCaseDataRepository.saveAndFlush(draft);
        entityManager.clear();

        assertThat(column(id, "created_at")).isEqualTo(created);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT updated_at > now() - interval '1 day' FROM draft.draft_case_data WHERE id = ?",
            Boolean.class, id)).isTrue();
    }

    @Test
    @DisplayName("a new draft is last saved when it is created")
    void newDraftIsLastSavedOnCreation() {
        DraftCaseDataEntity draft = new DraftCaseDataEntity();
        draft.setCaseReference(8_100_000_000_000_005L);
        draft.setEventId(EventId.enforceTheOrder);
        draft.setIdamUserId("user-1");
        draft.setCaseData("{}");
        int id = draftCaseDataRepository.saveAndFlush(draft).getId();
        entityManager.clear();

        assertThat(draftCaseDataRepository.findById(id).orElseThrow().getUpdatedAt()).isNotNull();
    }

    /**
     * A draft created and last saved the given number of days ago, written straight to the table so that the
     * dates are the test's rather than Hibernate's.
     */
    private int draft(long caseReference, EventId eventId, int createdDaysAgo, int savedDaysAgo) {
        DraftCaseDataEntity draft = new DraftCaseDataEntity();
        draft.setCaseReference(caseReference);
        draft.setEventId(eventId);
        draft.setIdamUserId("user-" + caseReference);
        draft.setCaseData("{}");
        int id = draftCaseDataRepository.saveAndFlush(draft).getId();
        jdbcTemplate.update("""
            UPDATE draft.draft_case_data
            SET created_at = now() - make_interval(days => ?), updated_at = now() - make_interval(days => ?)
            WHERE id = ?""", createdDaysAgo, savedDaysAgo, id);
        entityManager.clear();
        return id;
    }

    private Timestamp column(int id, String column) {
        return jdbcTemplate.queryForObject(
            "SELECT " + column + " FROM draft.draft_case_data WHERE id = ?", Timestamp.class, id);
    }
}
