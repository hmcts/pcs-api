package uk.gov.hmcts.reform.pcs.ccd.service.coversheet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoversheetProviderTest {

    @Mock
    private CoversheetPayloadBuilder coversheetPayloadBuilder;
    @Mock
    private CoversheetDocumentGenerator coversheetDocumentGenerator;
    @Mock
    private DocumentIdExtractor documentIdExtractor;
    @Mock
    private LetterDocumentFetcher letterDocumentFetcher;

    @InjectMocks
    private CoversheetProvider underTest;

    @Test
    @DisplayName("Builds the payload, generates then fetches the coversheet bytes")
    void shouldBuildPayloadGenerateThenFetchCoversheet() {
        AddressUK address = AddressUK.builder().addressLine1("1 High Street").build();
        CoversheetPayload payload = CoversheetPayload.builder().build();
        UUID coversheetId = UUID.randomUUID();
        byte[] coversheet = "coversheet".getBytes();

        when(coversheetPayloadBuilder.build("Jane Doe", address, "1234-5678-9012-3456")).thenReturn(payload);
        when(coversheetDocumentGenerator.generate(payload)).thenReturn("http://dm-store/documents/cover");
        when(documentIdExtractor.extractDocumentId("http://dm-store/documents/cover")).thenReturn(coversheetId);
        when(letterDocumentFetcher.fetchBytes(coversheetId)).thenReturn(coversheet);

        byte[] result = underTest.render("Jane Doe", address, "1234-5678-9012-3456");

        assertThat(result).isSameAs(coversheet);
        verify(letterDocumentFetcher).fetchBytes(coversheetId);
    }
}
