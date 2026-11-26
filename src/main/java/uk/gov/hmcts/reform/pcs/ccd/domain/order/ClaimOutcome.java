package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * What an issued order decides about the claim, the same however its wording was written: the frontend derives it
 * from the judge's answers for an order its form generated, and court staff record it for one the judge wrote.
 */
@Embeddable
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClaimOutcome(@Enumerated(EnumType.STRING) Kind kind,
                           Boolean possessionForthwith,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate possessionDate,
                           @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime possessionTime,
                           @Enumerated(EnumType.STRING) Grounds grounds,
                           String groundsDetails,
                           Boolean warrantOnNotice) {

    public static ClaimOutcome of(Kind kind) {
        return new ClaimOutcome(kind, null, null, null, null, null, null);
    }

    /**
     * Whether it says what its kind needs: when possession is ordered. A time, which the order may leave out, is the
     * time on the day possession is ordered by, so only an order with that day has one.
     */
    public boolean complete() {
        return kind != null && (possessionTime == null || possessionDate != null) && switch (kind) {
            case OUTRIGHT_POSSESSION -> Boolean.TRUE.equals(possessionForthwith) == (possessionDate == null);
            case SUSPENDED_POSSESSION -> possessionDate != null;
            default -> true;
        };
    }

    public enum Kind {
        OUTRIGHT_POSSESSION,
        SUSPENDED_POSSESSION,
        STRUCK_OUT,
        DISMISSED,
        /** The order makes no order on the claim, as an adjournment, which manages the case without deciding it. */
        NO_ORDER,
        /** The judge's own order, which none of the others describes. */
        OTHER
    }

    public enum Grounds {
        MANDATORY,
        DISCRETIONARY
    }
}
