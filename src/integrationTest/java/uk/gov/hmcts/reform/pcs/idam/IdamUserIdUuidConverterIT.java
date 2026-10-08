package uk.gov.hmcts.reform.pcs.idam;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("IDAM id mapping against uuid and text columns")
class IdamUserIdUuidConverterIT extends AbstractPostgresContainerIT {

    private static final String IDAM_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @Order(1)
    @DisplayName("reads and writes IDAM ids when columns are still uuid")
    void roundTripsAgainstUuidColumns() {
        long caseReference = 9_178_000_001L;

        transactionTemplate.executeWithoutResult(status -> {
            saveCaseWithDefendant(caseReference, IDAM_ID);
            entityManager.flush();
            entityManager.clear();

            PartyEntity reloaded = loadDefendant(caseReference);
            assertThat(reloaded.getIdamId()).isEqualTo(IDAM_ID);
        });
    }

    @Test
    @Order(2)
    @DisplayName("still reads IDAM ids after columns were widened to text (Part 2 shape)")
    void readsAfterColumnsWidenedToText() {
        long caseReference = 9_178_000_002L;

        transactionTemplate.executeWithoutResult(status -> {
            saveCaseWithDefendant(caseReference, IDAM_ID);
            entityManager.flush();
        });

        jdbcTemplate.execute(
            "ALTER TABLE public.party ALTER COLUMN idam_id TYPE text USING idam_id::text");
        jdbcTemplate.execute(
            "ALTER TABLE draft.draft_case_data ALTER COLUMN idam_user_id TYPE text "
                + "USING idam_user_id::text");
        jdbcTemplate.execute(
            "ALTER TABLE public.draft_orders ALTER COLUMN author_idam_user_id TYPE text "
                + "USING author_idam_user_id::text");

        try {
            transactionTemplate.executeWithoutResult(status -> {
                entityManager.clear();
                PartyEntity reloaded = loadDefendant(caseReference);
                assertThat(reloaded.getIdamId()).isEqualToIgnoringCase(IDAM_ID);
            });
        } finally {
            jdbcTemplate.execute(
                "ALTER TABLE public.party ALTER COLUMN idam_id TYPE uuid USING idam_id::uuid");
            jdbcTemplate.execute(
                "ALTER TABLE draft.draft_case_data ALTER COLUMN idam_user_id TYPE uuid "
                    + "USING idam_user_id::uuid");
            jdbcTemplate.execute(
                "ALTER TABLE public.draft_orders ALTER COLUMN author_idam_user_id TYPE uuid "
                    + "USING author_idam_user_id::uuid");
        }
    }

    private void saveCaseWithDefendant(long caseReference, String idamUserId) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(caseReference);

        ClaimEntity claimEntity = ClaimEntity.builder().build();
        caseEntity.addClaim(claimEntity);

        PartyEntity party = new PartyEntity();
        party.setIdamId(idamUserId);
        party.setFirstName("John");
        party.setLastName("Doe");
        caseEntity.addParty(party);
        claimEntity.addParty(party, PartyRole.DEFENDANT);

        pcsCaseRepository.save(caseEntity);
    }

    private PartyEntity loadDefendant(long caseReference) {
        PcsCaseEntity caseEntity = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        return caseEntity.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(claimParty -> claimParty.getParty())
            .findFirst()
            .orElseThrow();
    }
}
