package uk.gov.hmcts.reform.pcs.document.model.enforcement;

import lombok.Builder;
import lombok.Data;

/**
 * A defendant's name and address on an enforcement application document.
 */
@Data
@Builder
public class EnforcementDefendant {

    private String name;
    private String address;

}
