package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HearingWindow {

    private LocalDate dateRangeStart;

    private LocalDate dateRangeEnd;

    private LocalDateTime firstDateTimeMustBe;
}
