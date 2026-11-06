package uk.gov.hmcts.reform.pcs.hearings.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class HearingValuesRequest {

    private Long caseReference;
    private String hearingId;
}