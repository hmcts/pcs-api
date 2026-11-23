package uk.gov.hmcts.reform.pcs.ccd.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace.BreathingSpaceRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class BreathingSpaceRepositoryIT extends AbstractPostgresContainerIT {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Autowired
    private BreathingSpaceRepository breathingSpaceRepository;

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnNewestBreathingSpaceFirst() {
        // Given
        PcsCaseEntity pcsCase = createPcsCase();

        insertBreathingSpace(
            pcsCase.getId(),
            "BS-OLD",
            LocalDateTime.of(2026, 10, 1, 10, 0)
        );

        insertBreathingSpace(
            pcsCase.getId(),
            "BS-NEW",
            LocalDateTime.of(2026, 11, 12, 10, 0)
        );

        entityManager.flush();
        entityManager.clear();

        // When
        List<BreathingSpaceEntity> results =
            breathingSpaceRepository.findByCaseReference(CASE_REFERENCE);

        // Then
        assertThat(results)
            .extracting(BreathingSpaceEntity::getReferenceNumber)
            .containsExactly("BS-NEW", "BS-OLD");
    }

    @Test
    void shouldPopulateCreatedAtWhenSavingNewBreathingSpace() {

        PcsCaseEntity pcsCase = createPcsCase();
        UUID id = UUID.randomUUID();

        // When
        jdbcTemplate.update(
            """
                INSERT INTO breathing_space
                    (id, pcs_case_id, reference_number)
                VALUES (?, ?, ?)
                """,
            id,
            pcsCase.getId(),
            "BS-DEFAULT"
        );

        entityManager.clear();

        // Then
        BreathingSpaceEntity reloaded =
            breathingSpaceRepository.findById(id).orElseThrow();

        assertThat(reloaded.getCreatedAt()).isNotNull();
    }


    private PcsCaseEntity createPcsCase() {
        return pcsCaseRepository.saveAndFlush(
            PcsCaseEntity.builder()
                .caseReference(CASE_REFERENCE)
                .build()
        );
    }


    private void insertBreathingSpace(
        UUID pcsCaseId,
        String referenceNumber,
        LocalDateTime createdAt
    ) {
        jdbcTemplate.update("""
            INSERT INTO breathing_space
                (id, pcs_case_id, reference_number, created_at)
            VALUES (?, ?, ?, ?)
            """,
                            UUID.randomUUID(),
                            pcsCaseId,
                            referenceNumber,
                            createdAt
        );
    }
}
