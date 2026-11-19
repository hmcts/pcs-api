package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PcsCaseHardDeleteRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHardDeleteDocumentsAndClearInboundDocumentForeignKeys() {
        UUID caseId = UUID.randomUUID();
        PcsCaseHardDeleteRepository underTest = new PcsCaseHardDeleteRepository(jdbcTemplate);

        underTest.deleteBlockingDependencies(caseId);

        verify(jdbcTemplate).update(eq("DELETE FROM document WHERE case_id = ?"), eq(caseId));
        verify(jdbcTemplate).update(
            ArgumentMatchers.contains("DELETE FROM claim_document"),
            eq(caseId)
        );
    }
}
