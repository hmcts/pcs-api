package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the order-document-generation db-scheduler task: the issued order whose document it
 * generates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDocumentTaskData {

    private String issuedOrderId;

}
