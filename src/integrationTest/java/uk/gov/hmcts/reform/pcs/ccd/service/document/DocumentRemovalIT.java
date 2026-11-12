package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.exception.DocumentNotFoundException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("integration")
@DisplayName("Document removal")
class DocumentRemovalIT extends AbstractPostgresContainerIT {

    @Autowired
    private DocumentRemovalService documentRemovalService;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    // The document store the removed document is deleted from.
    @MockitoBean
    private DocumentImportService documentImportService;

    @Test
    @DisplayName("soft deletes the document, recording why and when, and deletes it from the store")
    void softDeletesTheDocument() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(1781000000000301L);
        UUID cdamId = UUID.randomUUID();
        DocumentEntity document = DocumentEntity.builder()
            .documentId(cdamId)
            .fileName("duplicate.pdf")
            .url("http://dm-store/documents/" + cdamId)
            .build();
        pcsCase.addDocument(document);
        pcsCaseRepository.saveAndFlush(pcsCase);

        // The event submit handler supplies the transaction in production.
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            documentRemovalService.removeDocument(document.getId(), "Duplicate upload"));

        verify(documentImportService).deleteDocument(document.getUrl());
        Map<String, Object> row = jdbc.queryForMap(
            "select removed, removal_reason, removed_at from document where id = ?", document.getId());
        assertThat(row.get("removed")).isEqualTo(true);
        assertThat(row.get("removal_reason")).isEqualTo("Duplicate upload");
        assertThat(row.get("removed_at")).isNotNull();
        assertThat(documentRepository.findById(document.getId())).isEmpty();
    }

    @Test
    @DisplayName("rejects an unknown document without touching the store")
    void rejectsAnUnknownDocument() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> documentRemovalService.removeDocument(unknown, "Duplicate upload"))
            .isInstanceOf(DocumentNotFoundException.class);
        verifyNoInteractions(documentImportService);
    }
}
