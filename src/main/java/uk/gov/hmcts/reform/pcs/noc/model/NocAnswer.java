package uk.gov.hmcts.reform.pcs.noc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NocAnswer(
    @JsonProperty("question_id") String questionId,
    String value
) {
}
