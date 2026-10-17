package uk.gov.hmcts.reform.pcs.enforcement.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One warrant or writ on the claim, as shown on the "Enforcement status" tab. It is derived for the case view
 * from the enforcement record and is never stored.
 *
 * <p>The warrant / writ number and, for writs, the date of the judge's order approving it ("Date of transfer
 * to the High Court") are not recorded yet. They are left out until the "Decide enforcement" event stores them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnforcementSummary {

    @CCD(label = "Type")
    private SelectEnforcementType enforcementType;

    @CCD(label = "Status")
    private EnforcementStatus status;

    @CCD(label = "Status updated")
    private LocalDate statusUpdatedDate;

    /**
     * The eviction date booked with the bailiff. Warrants only: writs are enforced by a High Court Enforcement
     * Officer, not county court bailiffs.
     */
    @CCD(label = "Scheduled eviction")
    private LocalDateTime scheduledEvictionDate;
}
