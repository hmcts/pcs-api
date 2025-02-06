package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UnavailabilityRanges {

    private LocalDate unavailableFromDate;

    private LocalDate unavailableToDate;

    @JsonProperty("unavailabilityType")
    private String unavailabilityType;
}
