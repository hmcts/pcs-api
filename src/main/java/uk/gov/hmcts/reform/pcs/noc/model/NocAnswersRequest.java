package uk.gov.hmcts.reform.pcs.noc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record NocAnswersRequest(
    @JsonProperty("case_id") String caseId,
    List<NocAnswer> answers
) {
}
