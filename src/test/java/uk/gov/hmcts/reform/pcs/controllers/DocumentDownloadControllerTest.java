import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import uk.gov.hmcts.reform.pcs.controllers.DocumentDownloadController;
import uk.gov.hmcts.reform.pcs.document.model.DownloadedDocumentResponse;
import uk.gov.hmcts.reform.pcs.document.service.DocumentDownloadService;

class DocumentDownloadControllerVoidTest {

    private static final String DOCUMENT_ID = "12345678-1234-1234-1234-123456789abc";
    private static final String AUTHORIZATION = "Bearer token";

    @Mock
    private DocumentDownloadService documentDownloadService;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletOutputStream servletOutputStream;

    private DocumentDownloadController underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new DocumentDownloadController(documentDownloadService);
    }

    @Test
    void shouldStreamDocumentSuccessfully() throws IOException {
        // Given
        byte[] fileContent = "Hello world".getBytes();
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileContent));
        String fileName = "test-document.pdf";
        String mimeType = "application/pdf";

        DownloadedDocumentResponse documentResponse = new DownloadedDocumentResponse(
            resource,
            fileName,
            mimeType
        );

        when(documentDownloadService.downloadDocument(AUTHORIZATION, DOCUMENT_ID))
            .thenReturn(documentResponse);

        when(response.getOutputStream()).thenReturn(servletOutputStream);

        // When
        underTest.downloadDocumentById(AUTHORIZATION, DOCUMENT_ID, response);

        // Then
        // Verify headers set
        verify(response).setContentType(mimeType);
        verify(response).setHeader(eq("Content-Disposition"), contains(fileName));

        // Verify service called
        verify(documentDownloadService).downloadDocument(AUTHORIZATION, DOCUMENT_ID);

        // Verify streaming happened
        verify(servletOutputStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
        verify(servletOutputStream).flush();
        verify(servletOutputStream).close();
    }
}
