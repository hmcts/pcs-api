package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

@Data
@Builder
public class CaseSupportGenerationResponse {

    private State state;
    private List<String> errors;

}
