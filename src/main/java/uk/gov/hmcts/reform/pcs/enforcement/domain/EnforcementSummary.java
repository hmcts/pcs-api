package uk.gov.hmcts.reform.pcs.enforcement.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnforcementSummary {

    @CCD(label = "Type")
    private SelectEnforcementType enforcementType;

    @CCD(label = "Warrant or writ number")
    private String enforcementNumber;

    @CCD(label = "Status")
    private EnforcementStatus status;

    @CCD(label = "Status updated")
    private LocalDate statusUpdatedDate;

    @CCD(label = "Scheduled eviction")
    private LocalDateTime scheduledEvictionDate;

    @CCD(label = "Date of transfer to the High Court")
    private LocalDate highCourtTransferDate;
}
