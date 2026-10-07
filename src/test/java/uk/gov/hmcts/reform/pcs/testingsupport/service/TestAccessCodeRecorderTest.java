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
class TestAccessCodeRecorderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistingRecorderShouldInsertPlaintextPin() {
        PersistingTestAccessCodeRecorder underTest = new PersistingTestAccessCodeRecorder(jdbcTemplate);
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        underTest.record(caseId, partyId, "PLAINTEXTPIN1");

        verify(jdbcTemplate).update(
            "INSERT INTO testing_support_access_code (case_id, party_id, plaintext_code) VALUES (?, ?, ?)",
            caseId, partyId, "PLAINTEXTPIN1"
        );
    }

    @Test
    void noOpRecorderShouldNotPersistAnything() {
        NoOpTestAccessCodeRecorder underTest = new NoOpTestAccessCodeRecorder();

        underTest.record(UUID.randomUUID(), UUID.randomUUID(), "PLAINTEXTPIN1");

        verifyNoInteractions(jdbcTemplate);
    }
}
