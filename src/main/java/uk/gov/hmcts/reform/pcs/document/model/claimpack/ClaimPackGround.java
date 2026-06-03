package uk.gov.hmcts.reform.pcs.document.model.claimpack;

import lombok.Builder;
import lombok.Data;

/**
 * One row in §6.3.7 "Grounds for possession" repeat block.
 *
 * <p>{@code hasReason} drives the {@code R-GROUND-REQUIRES-REASON} per-row gate so the
 * "reason for claiming possession under ground X" row only renders when the user provided one.</p>
 */
@Data
@Builder
public class ClaimPackGround {

    /** Printable label, e.g. "Ground 8 (Rent arrears, mandatory)". */
    private String nameAndNumber;
    private String reasonFreeText;
    private boolean hasReason;

}
