package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestCaseDocumentServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    private static final String DOCUMENT_URL =
        "http://dm-store/documents/11111111-1111-1111-1111-111111111111";

    private static final String BINARY_URL =
        DOCUMENT_URL + "/binary";

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private TestCaseDocumentService underTest;

    @Test
    void shouldAddPossessionOrderPlaceholderToCase() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE))
            .thenReturn(pcsCaseEntity);

        // When
        underTest.addPossessionOrderPlaceholder(CASE_REFERENCE);

        // Then
        ArgumentCaptor<DocumentEntity> documentCaptor =
            ArgumentCaptor.forClass(DocumentEntity.class);

        verify(documentRepository)
            .save(documentCaptor.capture());

        DocumentEntity savedDocument = documentCaptor.getValue();

        assertThat(savedDocument.getType())
            .isEqualTo(DocumentType.POSSESSION_ORDER);

        assertThat(savedDocument.getFileName())
            .isEqualTo("Possession order.pdf");

        assertThat(savedDocument.getContentType())
            .isEqualTo("application/pdf");

        assertThat(savedDocument.getCategoryId())
            .isEqualTo(
                CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS.getId()
            );

        assertThat(savedDocument.getUrl())
            .isEqualTo(DOCUMENT_URL);

        assertThat(savedDocument.getBinaryUrl())
            .isEqualTo(BINARY_URL);

        assertThat(savedDocument.getDocumentId())
            .isNotNull();

        assertThat(savedDocument.getPcsCase())
            .isSameAs(pcsCaseEntity);

        assertThat(pcsCaseEntity.getDocuments())
            .containsExactly(savedDocument);

        verify(pcsCaseService)
            .loadCase(CASE_REFERENCE);
    }
}
