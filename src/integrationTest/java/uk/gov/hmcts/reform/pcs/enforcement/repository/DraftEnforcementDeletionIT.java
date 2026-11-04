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
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.DraftEnforcementDeletionService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Enforcement draft deletion by when the draft was created")
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
    @DisplayName("deletes a draft created before the cutoff, keeping a newer one and another journey's")
    void deletesByCreationDate() {
        final int createdBeforeTheCutoff = draft(8_100_000_000_000_001L, EventId.enforceTheOrder, 40);
        final int createdAfterTheCutoff = draft(8_100_000_000_000_002L, EventId.enforceTheOrder, 29);
        final int otherJourney = draft(8_100_000_000_000_003L, EventId.resumePossessionClaim, 40);

        draftEnforcementDeletionService.deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);
        entityManager.clear();

        assertThat(draftCaseDataRepository.existsById(createdBeforeTheCutoff)).isFalse();
        assertThat(draftCaseDataRepository.existsById(createdAfterTheCutoff)).isTrue();
        assertThat(draftCaseDataRepository.existsById(otherJourney)).isTrue();
    }

    @Test
    @DisplayName("deletes a draft created before the cutoff even though it was saved today")
    void deletesADraftCreatedBeforeTheCutoffThatWasSavedToday() {
        final int startedLongAgoSavedToday = draft(8_100_000_000_000_004L, EventId.enforceTheOrder, 90);

        DraftCaseDataEntity draft = draftCaseDataRepository.findById(startedLongAgoSavedToday).orElseThrow();
        draft.setCaseData("{\"answer\": \"changed\"}");
        draftCaseDataRepository.saveAndFlush(draft);
        entityManager.clear();

        draftEnforcementDeletionService.deleteEnforcementDraftsBatch(DISCARD_AFTER_DAYS);
        entityManager.clear();

        assertThat(draftCaseDataRepository.existsById(startedLongAgoSavedToday)).isFalse();
    }

    /**
     * Written straight to the table, so the creation date is the test's rather than Hibernate's.
     */
    private int draft(long caseReference, EventId eventId, int createdDaysAgo) {
        DraftCaseDataEntity draft = new DraftCaseDataEntity();
        draft.setCaseReference(caseReference);
        draft.setEventId(eventId);
        draft.setIdamUserId("user-" + caseReference);
        draft.setCaseData("{}");
        int id = draftCaseDataRepository.saveAndFlush(draft).getId();
        jdbcTemplate.update("""
            UPDATE draft.draft_case_data
            SET created_at = now() - make_interval(days => ?)
            WHERE id = ?""", createdDaysAgo, id);
        entityManager.clear();
        return id;
    }
}
