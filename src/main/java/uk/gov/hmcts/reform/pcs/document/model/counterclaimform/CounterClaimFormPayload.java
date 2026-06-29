package uk.gov.hmcts.reform.pcs.document.model.counterclaimform;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

import java.time.LocalDate;

/**
 * Typed payload rendered into the counter claim form Docmosis template
 * ({@code CV-PCS-CLM-ENG-Counterclaim-Form.docx}). One form is produced for the defendant who
 * raised the counterclaim.
 *
 * <p>Smoke-test scaffold: only the title-block fields are populated for now (see
 * {@code CounterClaimFormPayloadBuilder}). The full {@code counter_claim} data mapping (amount,
 * particulars, HWF, SoT line 4) is backfilled once the pipeline is proven end to end.</p>
 */
@Data
@Builder
public class CounterClaimFormPayload implements FormPayload {

    // ---------- Title block ----------
    private String referenceNumber;
    private LocalDate issueDateSealed;
    private LocalDate submittedOn;

    // TODO HDPI-6865: backfill claimant/defendant, amount block, particulars, HWF, SoT line 4.

}
