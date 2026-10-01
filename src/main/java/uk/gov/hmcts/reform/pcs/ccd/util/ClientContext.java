package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientContext {

    private String selectedPartyId;

    /** The order a caseworker chose to review, or a judge to change, from the case's orders tab. */
    private String orderId;
}
