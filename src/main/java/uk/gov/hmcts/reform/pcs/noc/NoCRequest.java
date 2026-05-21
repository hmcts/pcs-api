package uk.gov.hmcts.reform.pcs.noc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoCRequest {

    @JsonAlias("caseId")
    @JsonProperty("case_id")
    private String caseId;

    @JsonAlias("caseTypeId")
    @JsonProperty("case_type_id")
    private String caseTypeId;

    private List<NoCAnswer> answers;
}
