package uk.gov.hmcts.reform.pcs.noc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NocQuestion(
    @JsonProperty("case_type_id") String caseTypeId,
    Integer order,
    @JsonProperty("question_text") String questionText,
    @JsonProperty("answer_field_type") NocFieldType answerFieldType,
    @JsonProperty("display_context_parameter") String displayContextParameter,
    @JsonProperty("challenge_question_id") String challengeQuestionId,
    @JsonProperty("answer_field") String answerField,
    @JsonProperty("question_id") String questionId
) {
}
