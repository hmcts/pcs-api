package uk.gov.hmcts.reform.pcs.document.model.claimpack;

import lombok.Builder;
import lombok.Data;

/**
 * One row in the "Grounds for possession" repeat block.
 *
 * <p>{@code hasReason} gates the per-row "reason for claiming possession under ground X" row, so
 * it only renders when the user provided a reason.</p>
 */
@Data
@Builder
public class ClaimPackGround {

    /** Printable label, e.g. "Ground 8 (Rent arrears, mandatory)". */
    private String nameAndNumber;
    private String reasonFreeText;
    private boolean hasReason;

}
