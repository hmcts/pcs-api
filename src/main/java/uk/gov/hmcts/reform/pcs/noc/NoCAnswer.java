package uk.gov.hmcts.reform.pcs.noc;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NoCAnswer {

    @JsonAlias("questionId")
    @JsonProperty("question_id")
    private String questionId;

    private String value;
}
