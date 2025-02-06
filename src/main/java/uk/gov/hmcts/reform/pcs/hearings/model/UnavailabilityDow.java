package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnavailabilityDow {

    @JsonProperty("DOW")
    private String dow;

    @JsonProperty("DOWUnavailabilityType")
    private String dowUnavailabilityType;

}
