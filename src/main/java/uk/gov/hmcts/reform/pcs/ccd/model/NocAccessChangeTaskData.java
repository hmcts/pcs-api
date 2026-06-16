package uk.gov.hmcts.reform.pcs.ccd.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NocAccessChangeTaskData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String caseReference;
    private String userId;
    private NocAccessChangeAction action;
}
