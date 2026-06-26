package uk.gov.hmcts.reform.pcs.testingsupport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Non-production recorder that persists plaintext pins to {@code testing_support_pin} for the QA pins endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "pin-test-table.enabled", havingValue = "true")
public class PersistingTestPinRecorder implements TestPinRecorder {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void record(UUID caseId, UUID partyId, String plaintextCode) {
        jdbcTemplate.update(
            "INSERT INTO testing_support_pin (case_id, party_id, plaintext_code) VALUES (?, ?, ?)",
            caseId, partyId, plaintextCode
        );
        log.debug("Recorded testing-support pin for case {} and party {}", caseId, partyId);
    }
}
