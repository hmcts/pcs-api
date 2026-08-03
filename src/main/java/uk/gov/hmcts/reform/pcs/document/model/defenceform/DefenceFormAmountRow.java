package uk.gov.hmcts.reform.pcs.document.model.defenceform;

import lombok.Builder;
import lombok.Data;

/**
 * A single income or expense row on the defence form: a label and the pre-formatted amount and
 * frequency. Only rows the defendant selected are stored, so every row applies. {@code showAmount}
 * gates the amount/frequency cells so a row with no captured amount renders the label only.
 */
@Data
@Builder
public class DefenceFormAmountRow {

    private String label;
    private boolean showAmount;
    private String amount;
    private String frequency;

}
