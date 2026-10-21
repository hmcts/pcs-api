package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload;

import java.util.UUID;

/**
 * Payload for the order-document-generation task: the issued order and its case, the caseworker who
 * issued it, and what its document says, fixed when it was issued.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDocumentTaskData {

    private UUID orderId;
    private long caseReference;
    private String caseworkerId;
    private String caseworkerFirstName;
    private String caseworkerLastName;
    private OrderDocumentPayload payload;
}
