package uk.gov.hmcts.reform.pcs.bankholiday;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class BankHolidayEvent {
    private String title;
    private LocalDate date;
    private String notes;
    private boolean bunting;
}
