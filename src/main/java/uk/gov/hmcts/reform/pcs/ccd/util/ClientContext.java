package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ClientContext {

    private String selectedPartyId;
    private Map<String, Object> eventQueryParams;
}
