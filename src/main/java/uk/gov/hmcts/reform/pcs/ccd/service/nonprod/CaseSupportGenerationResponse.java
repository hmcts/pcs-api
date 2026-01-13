package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

public record CaseSupportGenerationResponse(State state, List<String> errors) {

}
