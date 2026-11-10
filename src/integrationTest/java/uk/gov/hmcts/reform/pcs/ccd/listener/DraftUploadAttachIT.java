package uk.gov.hmcts.reform.pcs.ccd.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Draft upload attach")
class DraftUploadAttachIT extends AbstractPostgresContainerIT {

    private static final long CASE_REFERENCE = 1781000000000401L;

    @Autowired
    private DraftCaseDataRepository draftCaseDataRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private CaseDocumentClientApi caseDocumentClientApi;

    // The executor runs in this profile; a failing attach stays queued, so the assertions cannot race it.
    @BeforeEach
    void failEveryAttach() {
        when(caseDocumentClientApi.patchDocument(any(), any(), any()))
            .thenThrow(new IllegalStateException("CDAM is not available"));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM scheduled_tasks WHERE task_instance LIKE ?", "draft-" + CASE_REFERENCE + "-%");
        jdbc.update("DELETE FROM draft.draft_case_data WHERE case_reference = ?", CASE_REFERENCE);
    }

    @Test
    @DisplayName("queues an attach for each upload once, when the draft is created and when it is updated")
    void queuesAnAttachForEachNewUpload() {
        UUID first = UUID.randomUUID();
        DraftCaseDataEntity draft = new DraftCaseDataEntity();
        draft.setCaseReference(CASE_REFERENCE);
        draft.setEventId(resumePossessionClaim);
        draft.setIdamUserId(UUID.randomUUID().toString());
        draft.setCaseData("{\"first\": " + upload(first) + "}");
        Integer id = draftCaseDataRepository.saveAndFlush(draft).getId();

        assertThat(queuedAttaches()).containsExactly("draft-" + CASE_REFERENCE + "-" + first);

        UUID second = UUID.randomUUID();
        jdbc.update("DELETE FROM scheduled_tasks WHERE task_instance LIKE ?", "draft-" + CASE_REFERENCE + "-%");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            DraftCaseDataEntity loaded = draftCaseDataRepository.findById(id).orElseThrow();
            loaded.setCaseData("{\"first\": " + upload(first) + ", \"second\": " + upload(second) + "}");
        });

        assertThat(queuedAttaches()).containsExactly("draft-" + CASE_REFERENCE + "-" + second);
    }

    private List<String> queuedAttaches() {
        return jdbc.queryForList(
            "SELECT task_instance FROM scheduled_tasks WHERE task_name = 'attach-document-task' "
                + "AND task_instance LIKE ?", String.class, "draft-" + CASE_REFERENCE + "-%");
    }

    private static String upload(UUID documentId) {
        return "{\"document_url\": \"http://dm-store/documents/" + documentId + "\", \"document_hash\": \"hash\"}";
    }
}
