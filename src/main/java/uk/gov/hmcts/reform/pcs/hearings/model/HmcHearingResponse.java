package uk.gov.hmcts.reform.pcs.hearings.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HmcHearingResponse {
    private String hmctsServiceCode;

    private String caseRef;

    private String hearingID;

    private HmcHearingUpdate hearingUpdate;
}
