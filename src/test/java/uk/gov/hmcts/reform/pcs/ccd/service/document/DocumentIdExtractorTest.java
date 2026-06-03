package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class DocumentIdExtractorTest {

    private DocumentIdExtractor underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentIdExtractor();
    }

    @Test
    void shouldExtractDocumentId() {
        // Given
        UUID expectedDocumentId = UUID.randomUUID();

        // When
        UUID actualDocumentId = underTest.extractDocumentId("https://test.com/" + expectedDocumentId);

        // Then
        assertThat(actualDocumentId).isEqualTo(expectedDocumentId);
    }

    @Test
    void shouldPropagateExceptionForInvalidDocumentUrl() {
        // When
        Throwable throwable = catchThrowable(() -> underTest.extractDocumentId("https://test.com/not-a-uuid"));

        // Then
        assertThat(throwable)
            .isInstanceOf(IllegalArgumentException.class);
    }

}
