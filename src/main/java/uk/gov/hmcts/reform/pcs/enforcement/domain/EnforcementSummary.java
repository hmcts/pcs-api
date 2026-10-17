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
 * <p>The warrant / writ number and, for writs, the date of transfer to the High Court are set once the
 * caseworker approves the application in "Decide enforcement". Both are unset until then.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnforcementSummary {

    @CCD(label = "Type")
    private SelectEnforcementType enforcementType;

    /**
     * The warrant or writ number given when the caseworker approves the application.
     */
    @CCD(label = "Warrant or writ number")
    private String enforcementNumber;

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

    /**
     * The date the caseworker approved the writ, which is when it transfers to the High Court. Writs only.
     */
    @CCD(label = "Date of transfer to the High Court")
    private LocalDate highCourtTransferDate;
}
