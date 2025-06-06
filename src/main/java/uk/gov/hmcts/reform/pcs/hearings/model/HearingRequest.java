package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HearingRequest {

    private HearingDetails hearingDetails;

    private CaseDetails caseDetails;

    private List<PartyDetails> partyDetails;

    private RequestDetails requestDetails;
}
