package uk.gov.hmcts.reform.pcs.noc;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NocAnswersRequest(
    @JsonProperty("case_id") long caseId,
    List<NocAnswer> answers
) {
}
