package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MapperConfigTest {

    private ModelMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new MapperConfig().modelMapper();
    }

    @Test
    void shouldMapDocumentEntityToDocument() {
        // Given
        String filename = "current filename";
        String url = "document url";
        String binaryUrl = "document binary url";
        String categoryId = CaseFileCategory.HEARING_DOCUMENTS.getId();

        DocumentEntity documentEntity = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .originalFileName("some original filename")
            .fileName(filename)
            .documentId(UUID.randomUUID())
            .type(DocumentType.OTHER)
            .categoryId(categoryId)
            .url(url)
            .binaryUrl(binaryUrl)
            .build();

        // When
        Document convertedDocument = underTest.map(documentEntity, Document.class);

        // Then
        assertThat(convertedDocument.getFilename()).isEqualTo(filename);
        assertThat(convertedDocument.getUrl()).isEqualTo(url);
        assertThat(convertedDocument.getBinaryUrl()).isEqualTo(binaryUrl);
        assertThat(convertedDocument.getCategoryId()).isEqualTo(categoryId);
    }

}
