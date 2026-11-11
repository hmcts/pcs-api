package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * What an issued order decides about the claim, the same however its wording was written: the frontend derives it
 * from the judge's answers for an order its form generated, and court staff record it for one the judge wrote.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClaimOutcome(Kind kind,
                           Boolean possessionForthwith,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate possessionDate,
                           Grounds grounds,
                           String groundsDetails,
                           Boolean moneyJudgment,
                           Boolean transferToHighCourt,
                           Condition condition,
                           Boolean warrantOnNotice,
                           Adjournment adjournment) {

    public static ClaimOutcome of(Kind kind) {
        return new ClaimOutcome(kind, null, null, null, null, null, null, null, null, null);
    }

    /** Whether it says what its kind needs: when possession is ordered, or how the claim is adjourned. */
    public boolean complete() {
        return kind != null && switch (kind) {
            case OUTRIGHT_POSSESSION -> Boolean.TRUE.equals(possessionForthwith) == (possessionDate == null);
            case SUSPENDED_POSSESSION -> possessionDate != null;
            case ADJOURNMENT -> adjournment != null;
            default -> true;
        };
    }

    public enum Kind {
        OUTRIGHT_POSSESSION,
        SUSPENDED_POSSESSION,
        ADJOURNMENT,
        STRUCK_OUT,
        DISMISSED,
        /** The order makes no order on the claim. */
        NO_ORDER,
        /** The judge's own order, which none of the others describes. */
        OTHER
    }

    public enum Grounds {
        MANDATORY,
        DISCRETIONARY
    }

    public enum Condition {
        MONEY,
        NON_MONEY
    }

    public enum Adjournment {
        FURTHER_HEARING,
        GENERALLY
    }
}
