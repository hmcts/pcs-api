package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
class DraftClaimDeletionServiceIT extends AbstractPostgresContainerIT {

    @Autowired
    private DraftClaimDeletionService underTest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS ccd");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS ccd.case_data (
                reference BIGINT PRIMARY KEY,
                state VARCHAR(70)
            )
            """);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldHardDeleteDraftClaimAndAssociatedRows(boolean submittedClaim) {
        TestData testData = insertDraftClaim(submittedClaim);

        underTest.deleteDraftClaim(testData.caseReference());

        assertNoRows("SELECT COUNT(*) FROM draft.draft_case_data WHERE case_reference = ?", testData.caseReference());
        assertNoRows("SELECT COUNT(*) FROM ccd.case_data WHERE reference = ?", testData.caseReference());
        assertNoRows("SELECT COUNT(*) FROM pcs_case WHERE id = ?", testData.caseId());
        assertNoRows("SELECT COUNT(*) FROM claim WHERE id = ?", testData.claimId());
        assertNoRows("SELECT COUNT(*) FROM party WHERE id = ?", testData.partyId());
        assertNoRows("SELECT COUNT(*) FROM fee_payment WHERE id = ?", testData.feePaymentId());
        assertNoRows("SELECT COUNT(*) FROM general_application WHERE id = ?", testData.generalApplicationId());
        assertNoRows("SELECT COUNT(*) FROM help_with_fees WHERE id = ?", testData.feeHelpWithFeesId());
        assertNoRows("SELECT COUNT(*) FROM help_with_fees WHERE id = ?", testData.generalApplicationHelpWithFeesId());
        assertNoRows("SELECT COUNT(*) FROM legal_representative WHERE id = ?", testData.legalRepresentativeId());
        assertNoRows("SELECT COUNT(*) FROM contact_preferences WHERE id = ?", testData.contactPreferencesId());
        assertNoRows("SELECT COUNT(*) FROM address WHERE id = ?", testData.propertyAddressId());
        assertNoRows("SELECT COUNT(*) FROM address WHERE id = ?", testData.partyAddressId());
        assertNoRows("SELECT COUNT(*) FROM address WHERE id = ?", testData.legalRepresentativeAddressId());
    }

    private TestData insertDraftClaim(boolean submittedClaim) {
        long caseReference = ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L, 9_999_999_999_999_999L);
        UUID caseId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID propertyAddressId = UUID.randomUUID();
        UUID partyAddressId = UUID.randomUUID();
        UUID legalRepresentativeAddressId = UUID.randomUUID();
        UUID contactPreferencesId = UUID.randomUUID();
        UUID legalRepresentativeId = UUID.randomUUID();
        UUID feeHelpWithFeesId = UUID.randomUUID();
        UUID generalApplicationHelpWithFeesId = UUID.randomUUID();
        UUID feePaymentId = UUID.randomUUID();
        UUID generalApplicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO address (id, version, address_line1) VALUES (?, 1, ?)",
                            propertyAddressId, "Property address");
        jdbcTemplate.update("INSERT INTO address (id, version, address_line1) VALUES (?, 1, ?)",
                            partyAddressId, "Party address");
        jdbcTemplate.update("INSERT INTO address (id, version, address_line1) VALUES (?, 1, ?)",
                            legalRepresentativeAddressId, "Legal representative address");
        jdbcTemplate.update("INSERT INTO pcs_case (id, version, case_reference, property_address_id) VALUES (?, 1, ?, ?)",
                            caseId, caseReference, propertyAddressId);
        jdbcTemplate.update("INSERT INTO contact_preferences (id) VALUES (?)", contactPreferencesId);
        jdbcTemplate.update("""
            INSERT INTO party (id, version, case_id, type, idam_id, address_id, contact_preferences_id)
            VALUES (?, 1, ?, 'CLAIMANT', ?, ?, ?)
            """, partyId, caseId, userId, partyAddressId, contactPreferencesId);
        jdbcTemplate.update("""
            INSERT INTO claim (id, version, case_id, claim_submitted_date, claim_issued_date)
            VALUES (?, 1, ?, ?, NULL)
            """, claimId, caseId, submittedClaim ? Timestamp.from(Instant.now()) : null);
        jdbcTemplate.update("""
            INSERT INTO legal_representative (id, address_id, first_name, last_name)
            VALUES (?, ?, 'Legal', 'Rep')
            """, legalRepresentativeId, legalRepresentativeAddressId);
        jdbcTemplate.update("""
            INSERT INTO claim_party_legal_representative (party_id, legal_representative_id)
            VALUES (?, ?)
            """, partyId, legalRepresentativeId);
        jdbcTemplate.update("INSERT INTO help_with_fees (id, hwf_reference) VALUES (?, 'HWF-FEE')",
                            feeHelpWithFeesId);
        jdbcTemplate.update("INSERT INTO help_with_fees (id, hwf_reference) VALUES (?, 'HWF-GA')",
                            generalApplicationHelpWithFeesId);
        jdbcTemplate.update("""
            INSERT INTO fee_payment (
                id, possession_claim_id, party_id, request_date, hwf_id, payment_callback_handler_type
            )
            VALUES (?, ?, ?, now(), ?, 'CLAIM_ISSUE')
            """, feePaymentId, claimId, partyId, feeHelpWithFeesId);
        jdbcTemplate.update("""
            INSERT INTO general_application (id, case_id, hwf_id, type, party_id, rank)
            VALUES (?, ?, ?, 'APPLICATION', ?, 1)
            """, generalApplicationId, caseId, generalApplicationHelpWithFeesId, partyId);
        jdbcTemplate.update("""
            INSERT INTO draft.draft_case_data (id, case_reference, case_data, event_id, idam_user_id, party_id)
            VALUES (?, ?, '{}'::jsonb, 'resumePossessionClaim', ?, ?)
            """, UUID.randomUUID(), caseReference, userId, partyId);
        jdbcTemplate.update("""
            INSERT INTO ccd.case_data (id, reference, jurisdiction, case_type_id, state, security_classification, data)
            VALUES (?, ?, 'PCS', 'PCS', 'AWAITING_SUBMISSION_TO_HMCTS', 'PUBLIC', '{}'::jsonb)
            """, ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE), caseReference);

        return new TestData(
            caseReference,
            caseId,
            claimId,
            partyId,
            propertyAddressId,
            partyAddressId,
            legalRepresentativeAddressId,
            contactPreferencesId,
            legalRepresentativeId,
            feeHelpWithFeesId,
            generalApplicationHelpWithFeesId,
            feePaymentId,
            generalApplicationId
        );
    }

    private void assertNoRows(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        assertThat(count).isZero();
    }

    private record TestData(
        long caseReference,
        UUID caseId,
        UUID claimId,
        UUID partyId,
        UUID propertyAddressId,
        UUID partyAddressId,
        UUID legalRepresentativeAddressId,
        UUID contactPreferencesId,
        UUID legalRepresentativeId,
        UUID feeHelpWithFeesId,
        UUID generalApplicationHelpWithFeesId,
        UUID feePaymentId,
        UUID generalApplicationId
    ) {
    }
}
