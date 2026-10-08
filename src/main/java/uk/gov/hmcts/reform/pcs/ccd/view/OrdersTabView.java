package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.OrdersTab;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.OrdersTableRenderer;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrdersTabView {

    private final UploadTimestampProvider uploadTimestampProvider;
    private final OrdersTableRenderer ordersTableRenderer;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        pcsCase.setOrdersTab(null);

        getPossessionOrder(pcsCaseEntity)
            .ifPresent(documentEntity -> setOrdersTabFields(pcsCase, documentEntity));
    }

    private void setOrdersTabFields(PCSCase pcsCase, DocumentEntity documentEntity) {
        LocalDateTime uploadedDate = uploadTimestampProvider.uploadTimestamp(documentEntity);

        OrdersTab ordersTab = OrdersTab.builder()
            .possessionOrder(toDocument(documentEntity))
            .possessionOrderUploadedDate(uploadedDate)
            .tableMarkup(ordersTableRenderer.render(documentEntity, uploadedDate))
            .build();

        pcsCase.setOrdersTab(ordersTab);
    }

    private static Optional<DocumentEntity> getPossessionOrder(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getDocuments().stream()
            .filter(OrdersTabView::isPossessionOrder)
            .filter(DocumentsView::isNotRemoved)
            .findFirst();
    }

    private static boolean isPossessionOrder(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.POSSESSION_ORDER;
    }

    private static Document toDocument(DocumentEntity documentEntity) {
        return Document.builder()
            .filename(documentEntity.getFileName())
            .url(documentEntity.getUrl())
            .binaryUrl(documentEntity.getBinaryUrl())
            .build();
    }
}
