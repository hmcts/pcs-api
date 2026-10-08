package uk.gov.hmcts.reform.pcs.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WaTask {

    private final String id;
    private final String type;

}
