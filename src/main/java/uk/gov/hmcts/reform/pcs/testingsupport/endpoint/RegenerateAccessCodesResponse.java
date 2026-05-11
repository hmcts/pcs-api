package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import java.util.List;
import java.util.UUID;

public record RegenerateAccessCodesResponse(List<DefendantPin> pins) {

    public record DefendantPin(UUID partyId, String accessCode) {}
}
