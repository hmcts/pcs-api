package uk.gov.hmcts.reform.pcs.document.model;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DownloadedDocumentResponseTest {

    @Test
    void shouldCreateRecordWithAllFields() {
        // Given
        Resource mockResource = mock(Resource.class);
        String fileName = "test-document.pdf";
        String mimeType = "application/pdf";

        // When
        DownloadedDocumentResponse response = new DownloadedDocumentResponse(
            mockResource,
            fileName,
            mimeType
        );

        // Then
        assertThat(response.file()).isEqualTo(mockResource);
        assertThat(response.fileName()).isEqualTo(fileName);
        assertThat(response.mimeType()).isEqualTo(mimeType);
    }
}
