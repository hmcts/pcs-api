package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GetHearingResponse {

    private RequestDetails requestDetails;
    private HearingDetails hearingDetails;
    private CaseDetails caseDetails;
    private List<PartyDetails> partyDetails;
    private HearingResponse hearingResponse;
}
