package uk.gov.hmcts.reform.pcs.noc;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NocAnswer(
    @JsonProperty("question_id") String questionId,
    String value
) {
}
