package uk.gov.hmcts.reform.pcs.testingsupport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TestPinRecorderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistingRecorderShouldInsertPlaintextPin() {
        PersistingTestPinRecorder underTest = new PersistingTestPinRecorder(jdbcTemplate);
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        underTest.record(caseId, partyId, "PLAINTEXTPIN1");

        verify(jdbcTemplate).update(
            "INSERT INTO testing_support_pin (case_id, party_id, plaintext_code) VALUES (?, ?, ?)",
            caseId, partyId, "PLAINTEXTPIN1"
        );
    }

    @Test
    void noOpRecorderShouldNotPersistAnything() {
        NoOpTestPinRecorder underTest = new NoOpTestPinRecorder();

        underTest.record(UUID.randomUUID(), UUID.randomUUID(), "PLAINTEXTPIN1");

        verifyNoInteractions(jdbcTemplate);
    }
}
