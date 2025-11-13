package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ASBQuestionsWales {

    private String antisocialBehaviourDetails;
    private String illegalPurposesUseDetails;
    private String otherProhibitedConductDetails;

}

