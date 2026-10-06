package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.OrdersTab;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

@Component
@RequiredArgsConstructor
public class OrdersTabView {

    private final UploadTimestampProvider uploadTimestampProvider;

    public void setCaseFields(
        PCSCase pcsCase,
        PcsCaseEntity pcsCaseEntity
    ) {
        pcsCaseEntity.getDocuments().stream()
            .filter(DocumentsView::isNotRemoved)
            .filter(this::isPossessionOrder)
            .findFirst()
            .ifPresent(documentEntity ->
                           pcsCase.setOrdersTab(
                               OrdersTab.builder()
                                   .possessionOrder(toDocument(documentEntity))
                                   .possessionOrderUploadedDate(
                                       uploadTimestampProvider.uploadTimestamp(documentEntity)
                                   )
                                   .build()
                           )
            );
    }

    private boolean isPossessionOrder(DocumentEntity document) {
        return document.getType() == DocumentType.POSSESSION_ORDER;
    }

    private Document toDocument(DocumentEntity entity) {
        return Document.builder()
            .filename(entity.getFileName())
            .url(entity.getUrl())
            .binaryUrl(entity.getBinaryUrl())
            .build();
    }

}
