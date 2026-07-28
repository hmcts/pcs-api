package uk.gov.hmcts.reform.pcs.bankholiday;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class BankHolidayResponse {
    private String division;
    private List<BankHolidayEvent> events;

    public Set<LocalDate> getDates() {
        if (CollectionUtils.isEmpty(events)) {
            return Collections.emptySet();
        }

        return events.stream()
            .map(BankHolidayEvent::getDate)
            .collect(Collectors.toSet());
    }
}
