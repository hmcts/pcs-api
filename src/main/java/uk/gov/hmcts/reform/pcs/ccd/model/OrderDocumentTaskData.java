package uk.gov.hmcts.reform.pcs.ccd.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Payload for the order-document-generation task: the issued order, and the caseworker who issued it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDocumentTaskData {

    private UUID orderId;
    private String caseworkerId;
    private String caseworkerFirstName;
    private String caseworkerLastName;
}
