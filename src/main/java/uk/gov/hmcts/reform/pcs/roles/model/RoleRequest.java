package uk.gov.hmcts.reform.pcs.roles.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.roles.model.enums.RequestType;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequest {

    private String assignerId;
    private RequestType requestType;
    private String process;
    private String reference;
    private boolean replaceExisting;

}
