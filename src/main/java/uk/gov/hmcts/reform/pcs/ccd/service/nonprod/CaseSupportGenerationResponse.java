package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import lombok.Builder;
import lombok.Data;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

@Data
@Builder
@Profile({"local", "dev", "preview"})
public class CaseSupportGenerationResponse {

    private State state;
    private List<String> errors;

}
