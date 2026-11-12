package uk.gov.hmcts.reform.pcs.ccd.domain.order;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A sum an order says the debtors pay the creditors, each one of the case's parties, and how: a one-off payment, the
 * whole or the balance by a date, instalments, or any of these. More than one debtor are jointly and severally liable.
 */
public record MoneyJudgment(BigDecimal arrears,
                            BigDecimal otherAmount,
                            BigDecimal costs,
                            BigDecimal oneOffAmount,
                            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate oneOffDate,
                            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate payBy,
                            BigDecimal instalmentAmount,
                            InstalmentFrequency instalmentFrequency,
                            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate firstInstalmentDate,
                            boolean suspended,
                            List<UUID> debtors,
                            List<UUID> creditors) {

    public MoneyJudgment {
        debtors = debtors == null ? List.of() : debtors;
        creditors = creditors == null ? List.of() : creditors;
    }

    /**
     * Whether it says what a judgment needs: a sum, none negative; a debtor and a creditor, no party both; and each
     * way of paying in full.
     */
    public boolean complete() {
        Set<UUID> parties = new HashSet<>(debtors);
        return Stream.of(arrears, otherAmount, costs, oneOffAmount, instalmentAmount)
                .filter(Objects::nonNull).allMatch(MoneyJudgment::inPence)
            && Stream.of(arrears, otherAmount, costs).anyMatch(Objects::nonNull)
            && Stream.of(arrears, otherAmount, costs).filter(Objects::nonNull).allMatch(sum -> sum.signum() >= 0)
            && !debtors.isEmpty() && !creditors.isEmpty()
            && creditors.stream().noneMatch(parties::contains)
            && (oneOffAmount == null) == (oneOffDate == null)
            && (oneOffAmount == null || oneOffAmount.signum() > 0)
            && Stream.of(instalmentAmount, instalmentFrequency, firstInstalmentDate).allMatch(Objects::isNull)
                != Stream.of(instalmentAmount, instalmentFrequency, firstInstalmentDate).allMatch(Objects::nonNull)
            && (instalmentAmount == null || instalmentAmount.signum() > 0);
    }

    /** A sum in pounds and pence that the database holds as it is. */
    private static boolean inPence(BigDecimal sum) {
        BigDecimal pence = sum.stripTrailingZeros();
        return Math.max(pence.scale(), 0) <= 2 && pence.precision() - pence.scale() <= 16;
    }

    public enum InstalmentFrequency {
        WEEKLY,
        FORTNIGHTLY,
        MONTHLY
    }
}
