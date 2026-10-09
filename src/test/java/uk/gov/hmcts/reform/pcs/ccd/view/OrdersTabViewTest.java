package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.OrdersTableRenderer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdersTabViewTest {

    private static final String DOCUMENT_URL =
        "http://dm-store/documents/11111111-1111-1111-1111-111111111111";

    private static final String BINARY_URL =
        DOCUMENT_URL + "/binary";

    @Mock
    private UploadTimestampProvider uploadTimestampProvider;
    @Mock
    private OrdersTableRenderer ordersTableRenderer;

    @InjectMocks
    private OrdersTabView underTest;

    @Test
    void shouldPopulateOrdersTabWhenPossessionOrderExists() {
        // Given
        LocalDateTime uploadedDate =
            LocalDateTime.of(2026, 10, 6, 12, 30);

        DocumentEntity possessionOrder = DocumentEntity.builder()
            .type(DocumentType.POSSESSION_ORDER)
            .fileName("Possession order.pdf")
            .url(DOCUMENT_URL)
            .binaryUrl(BINARY_URL)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .documents(List.of(possessionOrder))
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(uploadTimestampProvider.uploadTimestamp(possessionOrder))
            .thenReturn(uploadedDate);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getOrdersTab()).isNotNull();

        assertThat(pcsCase.getOrdersTab().getPossessionOrder())
            .isNotNull();

        assertThat(pcsCase.getOrdersTab().getPossessionOrder().getFilename())
            .isEqualTo("Possession order.pdf");

        assertThat(pcsCase.getOrdersTab().getPossessionOrder().getUrl())
            .isEqualTo(DOCUMENT_URL);

        assertThat(pcsCase.getOrdersTab().getPossessionOrder().getBinaryUrl())
            .isEqualTo(BINARY_URL);

        assertThat(pcsCase.getOrdersTab().getPossessionOrderUploadedDate())
            .isEqualTo(uploadedDate);

        verify(uploadTimestampProvider)
            .uploadTimestamp(possessionOrder);
    }

    @Test
    void shouldNotPopulateOrdersTabWhenPossessionOrderDoesNotExist() {
        // Given
        DocumentEntity document = DocumentEntity.builder()
            .type(DocumentType.CLAIM)
            .fileName("claim.pdf")
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .documents(List.of(document))
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getOrdersTab()).isNull();
    }

    @Test
    void shouldIgnoreOtherDocumentsAndUsePossessionOrder() {
        // Given
        DocumentEntity otherDocument = DocumentEntity.builder()
            .type(DocumentType.CLAIM)
            .fileName("claim.pdf")
            .build();

        DocumentEntity possessionOrder = DocumentEntity.builder()
            .type(DocumentType.POSSESSION_ORDER)
            .fileName("Possession order.pdf")
            .url(DOCUMENT_URL)
            .binaryUrl(BINARY_URL)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .documents(List.of(otherDocument, possessionOrder))
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getOrdersTab()).isNotNull();

        assertThat(pcsCase.getOrdersTab()
                       .getPossessionOrder()
                       .getFilename())
            .isEqualTo("Possession order.pdf");
    }
}
