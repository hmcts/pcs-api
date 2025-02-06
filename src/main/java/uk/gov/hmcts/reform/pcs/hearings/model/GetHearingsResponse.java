package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GetHearingsResponse {

    @JsonProperty("hmctsServiceCode")
    private String hmctsServiceCode;

    private String caseRef;

    private List<CaseHearing> caseHearings;

}
