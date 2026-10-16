package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientContext {

    private String selectedPartyId;

    /** The order a caseworker is to review, or a judge to change, named by the link in their task. */
    private String orderId;
}
