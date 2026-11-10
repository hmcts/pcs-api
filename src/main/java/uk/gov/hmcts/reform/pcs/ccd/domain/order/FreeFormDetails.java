package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * What a judge's free-form order does, as court staff record it on issuing it, so the case can act on the order
 * without its wording changing. The frontend sends only the answers for its category; nothing reads them yet, so
 * only the category is checked here.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FreeFormDetails(Category category,
                              Boolean possessionForthwith,
                              @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate possessionDate,
                              Grounds grounds,
                              String groundsDetails,
                              Boolean moneyJudgment,
                              Boolean transferToHighCourt,
                              Condition condition,
                              Boolean warrantOnNotice,
                              Adjournment adjournment,
                              Outcome outcome) {

    public enum Category {
        OUTRIGHT_POSSESSION,
        SUSPENDED_POSSESSION,
        ADJOURNMENT,
        STRIKE_OUT,
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

    public enum Outcome {
        STRUCK_OUT,
        DISMISSED
    }
}
