package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientContext {

    private String selectedPartyId;

    /** The order a caseworker chose to review, from the case's draft orders tab. */
    private String orderId;
}
