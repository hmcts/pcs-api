package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.riskassessment;

import lombok.Value;

import java.util.List;

/** An answered question and any conditional information displayed beneath its answer. */
@Value
public class RiskAssessmentRow {
    String question;
    String answer;
    List<String> details;
}
