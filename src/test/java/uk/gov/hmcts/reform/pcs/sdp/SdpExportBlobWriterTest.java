package uk.gov.hmcts.reform.pcs.sdp;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SdpExportBlobWriterTest {

    private static final String BLOB_NAME = "2026/10/08/claims.json";

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Captor
    private ArgumentCaptor<BlobParallelUploadOptions> uploadOptionsCaptor;

    private SdpExportBlobWriter underTest;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new JacksonConfiguration().getMapper();
        underTest = new SdpExportBlobWriter(blobContainerClient, objectMapper);
    }

    @Test
    void shouldUploadPayloadAsJsonWithoutOverwriting() {
        // Given
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
        TestPayload payload = new TestPayload("PCS-123", LocalDate.of(2026, 10, 8), null);

        // When
        underTest.write(BLOB_NAME, payload);

        // Then
        verify(blobContainerClient).getBlobClient(BLOB_NAME);
        verify(blobClient).uploadWithResponse(uploadOptionsCaptor.capture(), isNull(), eq(Context.NONE));

        BlobParallelUploadOptions options = uploadOptionsCaptor.getValue();
        assertThat(uploadedBody(options))
            .isEqualTo("{\"caseReference\":\"PCS-123\",\"issuedOn\":\"2026-10-08\"}");
        assertThat(options.getHeaders().getContentType()).isEqualTo("application/json");
        assertThat(options.getRequestConditions().getIfNoneMatch()).isEqualTo("*");
    }

    private static String uploadedBody(BlobParallelUploadOptions options) {
        return new String(BinaryData.fromFlux(options.getDataFlux()).block().toBytes(), StandardCharsets.UTF_8);
    }

    private record TestPayload(String caseReference, LocalDate issuedOn, String notes) {
    }
}
