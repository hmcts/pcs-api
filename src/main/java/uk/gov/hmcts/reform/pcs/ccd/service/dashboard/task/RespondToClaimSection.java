package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum RespondToClaimSection {
    START_NOW_AND_DETAILS("startNowAndDetails", "Defendant.RTC.StartNowAndDetails", false),
    PERSONAL_DETAILS("personalDetails", "Defendant.RTC.PersonalDetails", false),
    DISPUTE_AND_TENANCY("disputeAndTenancy", "Defendant.RTC.DisputeAndTenancy", false),
    PAYMENTS("payments", "Defendant.RTC.Payments", true),
    SITUATION_AND_CIRCUMSTANCES("situationAndCircumstances", "Defendant.RTC.SituationAndCircumstances", false),
    INCOME_AND_EXPENDITURE("incomeAndExpenditure", "Defendant.RTC.IncomeAndExpenditure", false),
    UPLOAD_FILES("uploadFiles", "Defendant.RTC.UploadFiles", false);

    private final String sectionId;
    private final String templateId;
    private final boolean rentArrearsOnly;

    RespondToClaimSection(String sectionId, String templateId, boolean rentArrearsOnly) {
        this.sectionId = sectionId;
        this.templateId = templateId;
        this.rentArrearsOnly = rentArrearsOnly;
    }

    public String sectionId() {
        return sectionId;
    }

    public String templateId() {
        return templateId;
    }

    public boolean isApplicable(boolean paymentsApplicable) {
        return !rentArrearsOnly || paymentsApplicable;
    }

    public static Optional<RespondToClaimSection> fromSectionId(String rawSectionId) {
        if (rawSectionId == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(v -> v.sectionId.equals(rawSectionId))
            .findFirst();
    }

    public static List<RespondToClaimSection> applicableInOrder(boolean paymentsApplicable) {
        return Arrays.stream(values())
            .filter(section -> section.isApplicable(paymentsApplicable))
            .toList();
    }
}
