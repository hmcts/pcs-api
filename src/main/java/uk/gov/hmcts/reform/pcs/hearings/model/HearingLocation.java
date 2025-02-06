package uk.gov.hmcts.reform.pcs.hearings.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HearingLocation {

    private String locationType;

    private String locationId;

}
