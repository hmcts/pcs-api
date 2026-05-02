package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentsViewTest {

    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private DocumentsView underTest;

    @BeforeEach
    void setUp() {
        pcsCase = PCSCase.builder().build();

        underTest = new DocumentsView();
    }

    @Test
    void shouldReturnEmptyListWhenNoDocumentsExist() {
        // Given
        when(pcsCaseEntity.getDocuments()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getAllDocuments()).isEmpty();
    }

    @Test
    void shouldMapDocuments() {
        // Given
        DocumentEntity entity1 = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName("doc1.pdf")
            .url("url1")
            .build();

        DocumentEntity entity2 = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName("doc2.pdf")
            .url("url2")
            .build();

        when(pcsCaseEntity.getDocuments()).thenReturn(List.of(entity1,entity2));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        //Then
        assertThat(pcsCase.getAllDocuments()).hasSize(2);
        assertThat(pcsCase.getAllDocuments()).extracting(lv -> lv.getValue().getFilename())
            .containsExactly("doc1.pdf", "doc2.pdf");
    }

}
