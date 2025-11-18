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

    private Boolean antisocialBehaviour;
    private String antisocialBehaviourDetails;
    private Boolean illegalPurposesUse;
    private String illegalPurposesUseDetails;
    private Boolean otherProhibitedConduct;
    private String otherProhibitedConductDetails;

}

