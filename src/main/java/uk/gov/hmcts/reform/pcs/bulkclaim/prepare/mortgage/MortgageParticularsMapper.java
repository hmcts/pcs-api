package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.mortgage;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.AdditionalPayment;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.MortgageParticulars;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.Occupant;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PaymentHistoryLine;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.AdditionalPaymentType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NameType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.OriginalMortgageProviderType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ParticularsMortgagedType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Validates and maps the mortgage-specific particulars of a bulk claim and its occupants.
 */
@Component
public class MortgageParticularsMapper {

    static final int MAX_ADDITIONAL_PAYMENTS = 3;
    static final int REPAYMENT_TERMS_LIMIT = 950;
    static final int INTERESTED_PARTY_LIMIT = 250;
    static final int PAYMENT_REASON_LIMIT = 250;
    static final int PROVIDER_NAME_LIMIT = 100;
    static final int RECOVERY_STEPS_LIMIT = 500;

    private static final BigDecimal MAX_RATE = new BigDecimal("100");

    private final Clock ukClock;

    public MortgageParticularsMapper(@Qualifier("ukClock") Clock ukClock) {
        this.ukClock = ukClock;
    }

    /**
     * Validates the particulars and maps them to the claim extras.
     *
     * @param path path of the particulars element relative to the claim, for example {@code ParticularsOfClaim}
     * @param paymentHistory the already mapped payment history, used to derive the total arrears when the claim
     *                       does not state it
     */
    public MortgageParticulars map(ParticularsMortgagedType particulars,
                                   List<PaymentHistoryLine> paymentHistory,
                                   String path,
                                   BulkErrorCollector errors) {

        LocalDate today = LocalDate.now(ukClock);

        notInFuture(path + "/MortgageDate", particulars.getMortgageDate(), today, "Mortgage date", errors);
        notInFuture(path + "/DefaultNoticeDate", particulars.getDefaultNoticeDate(), today,
                    "Default notice date", errors);
        notInFuture(path + "/NoticeServedDate", particulars.getNoticeServedDate(), today,
                    "Notice served date", errors);
        notInFuture(path + "/TenancyDate", particulars.getTenancyDate(), today, "Tenancy date", errors);

        errors.requiredWithMaxLength(path + "/CurrentRepaymentTerms", particulars.getCurrentRepaymentTerms(),
                                     REPAYMENT_TERMS_LIMIT, "Current repayment terms");
        errors.maxLength(path + "/InterestedParty", particulars.getInterestedParty(), INTERESTED_PARTY_LIMIT,
                         "Interested party");
        errors.maxLength(path + "/RecoverySteps", particulars.getRecoverySteps(), RECOVERY_STEPS_LIMIT,
                         "Recovery steps");

        money(path + "/LoanAmount", particulars.getLoanAmount(), "Loan amount", errors);
        money(path + "/FullRepaymentAmount", particulars.getFullRepaymentAmount(), "Full repayment amount", errors);
        money(path + "/CostsAndAdministration", particulars.getCostsAndAdministration(),
              "Costs and administration", errors);
        money(path + "/OutstandingAmount", particulars.getOutstandingAmount(), "Outstanding amount", errors);
        money(path + "/TotalArrears", particulars.getTotalArrears(), "Total arrears", errors);
        rate(path + "/StartInterestRate", particulars.getStartInterestRate(), "Start interest rate", errors);
        rate(path + "/PreArrearsInterestRate", particulars.getPreArrearsInterestRate(),
             "Pre-arrears interest rate", errors);
        rate(path + "/ClaimStartInterestRate", particulars.getClaimStartInterestRate(),
             "Claim start interest rate", errors);

        List<AdditionalPayment> additionalPayments = additionalPayments(
            particulars.getAdditionalPayments(), path + "/AdditionalPayments", errors);
        OriginalMortgageProviderType provider = particulars.getOriginalMortgageProvider();
        if (provider != null) {
            validateProvider(provider, today, path + "/OriginalMortgageProvider", errors);
        }

        return new MortgageParticulars(
            particulars.getMortgageDate(),
            particulars.getDefaultNoticeDate(),
            particulars.getLoanAmount(),
            particulars.getCurrentRepaymentTerms(),
            particulars.getFullRepaymentDate(),
            particulars.getFullRepaymentAmount(),
            particulars.getCostsAndAdministration(),
            particulars.getOutstandingAmount(),
            particulars.getStartInterestRate(),
            particulars.getPreArrearsInterestRate(),
            particulars.getClaimStartInterestRate(),
            particulars.getInterestedParty(),
            particulars.getTenancyDate(),
            particulars.getNoticeServedDate(),
            totalArrears(particulars.getTotalArrears(), paymentHistory),
            StringUtils.trimToNull(particulars.getRecoverySteps()),
            particulars.isHasMonetaryJudgement(),
            provider != null ? provider.getMortgageProviderName() : null,
            provider != null ? provider.getMortgageBoughtDate() : null,
            provider != null ? provider.getDateDefendantNotified() : null,
            additionalPayments
        );
    }

    public List<Occupant> occupants(List<NameType> occupants, String elementName, BulkErrorCollector errors) {
        List<Occupant> mapped = new ArrayList<>();
        for (int i = 0; i < occupants.size(); i++) {
            NameType name = occupants.get(i);
            String path = BulkErrorCollector.indexed(elementName, i);
            errors.requiredWithMaxLength(path + "/FirstName", name.getFirstName(), BulkLimits.NAME,
                                         "Occupant first name");
            errors.requiredWithMaxLength(path + "/LastName", name.getLastName(), BulkLimits.NAME,
                                         "Occupant last name");
            mapped.add(new Occupant(name.getTitle(), name.getFirstName(), name.getLastName(),
                                    name.getOtherNames(), name.getInitials()));
        }
        return mapped;
    }

    /**
     * Payment history is mandatory for a mortgage claim, so an absent or empty one is an error.
     */
    public void requirePaymentHistory(List<PaymentHistoryLine> lines, String path, BulkErrorCollector errors) {
        if (lines == null || lines.isEmpty()) {
            errors.add(path, BulkErrorCollector.REQUIRED_CODE, "At least one payment history line is required");
        }
    }

    private static BigDecimal totalArrears(BigDecimal stated, List<PaymentHistoryLine> paymentHistory) {
        if (stated != null) {
            return stated;
        }
        return paymentHistory.stream()
            .filter(line -> line.paymentDueDate() != null && line.cumulativeArrears() != null)
            .max(Comparator.comparing(PaymentHistoryLine::paymentDueDate))
            .map(PaymentHistoryLine::cumulativeArrears)
            .orElse(null);
    }

    private List<AdditionalPayment> additionalPayments(List<AdditionalPaymentType> payments,
                                                       String elementName,
                                                       BulkErrorCollector errors) {
        if (payments.size() > MAX_ADDITIONAL_PAYMENTS) {
            errors.add(BulkErrorCollector.indexed(elementName, MAX_ADDITIONAL_PAYMENTS), BulkErrorCollector.NOT_ALLOWED,
                       "No more than " + MAX_ADDITIONAL_PAYMENTS + " additional payments are allowed");
        }
        List<AdditionalPayment> mapped = new ArrayList<>();
        for (int i = 0; i < payments.size(); i++) {
            AdditionalPaymentType payment = payments.get(i);
            String path = BulkErrorCollector.indexed(elementName, i);
            if (errors.required(path + "/AdditionalPaymentAmount", payment.getAdditionalPaymentAmount(),
                                "Additional payment amount")) {
                money(path + "/AdditionalPaymentAmount", payment.getAdditionalPaymentAmount(),
                      "Additional payment amount", errors);
            }
            errors.requiredWithMaxLength(path + "/AdditionalPaymentReason", payment.getAdditionalPaymentReason(),
                                         PAYMENT_REASON_LIMIT, "Additional payment reason");
            money(path + "/AdditionalPaymentArrears", payment.getAdditionalPaymentArrears(),
                  "Additional payment arrears", errors);
            mapped.add(new AdditionalPayment(payment.getAdditionalPaymentAmount(),
                                             payment.getAdditionalPaymentReason(),
                                             payment.getAdditionalPaymentArrears()));
        }
        return mapped;
    }

    private void validateProvider(OriginalMortgageProviderType provider,
                                  LocalDate today,
                                  String path,
                                  BulkErrorCollector errors) {
        errors.requiredWithMaxLength(path + "/MortgageProviderName", provider.getMortgageProviderName(),
                                     PROVIDER_NAME_LIMIT, "Original mortgage provider name");
        if (errors.required(path + "/MortgageBoughtDate", provider.getMortgageBoughtDate(),
                            "Date the mortgage was bought")) {
            notInFuture(path + "/MortgageBoughtDate", provider.getMortgageBoughtDate(), today,
                        "Date the mortgage was bought", errors);
        }
        if (errors.required(path + "/DateDefendantNotified", provider.getDateDefendantNotified(),
                            "Date the defendant was notified")) {
            notInFuture(path + "/DateDefendantNotified", provider.getDateDefendantNotified(), today,
                        "Date the defendant was notified", errors);
        }
    }

    private static void notInFuture(String path, LocalDate date, LocalDate today, String label,
                                    BulkErrorCollector errors) {
        if (date != null && date.isAfter(today)) {
            errors.add(path, BulkErrorCollector.NOT_IN_PAST, label + " cannot be in the future");
        }
    }

    private static void money(String path, BigDecimal value, String label, BulkErrorCollector errors) {
        if (value != null && value.signum() < 0) {
            errors.add(path, BulkErrorCollector.INVALID_VALUE, label + " cannot be negative");
        }
    }

    private static void rate(String path, BigDecimal value, String label, BulkErrorCollector errors) {
        if (value != null && (value.signum() < 0 || value.compareTo(MAX_RATE) > 0)) {
            errors.add(path, BulkErrorCollector.INVALID_VALUE, label + " must be between 0 and 100");
        }
    }
}
