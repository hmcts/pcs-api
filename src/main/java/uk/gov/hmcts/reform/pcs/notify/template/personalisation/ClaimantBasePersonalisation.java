package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class ClaimantBasePersonalisation implements TemplatePersonalisation {
    private final String toLineClaimantName;
    private final String caseNumber;
    private final String caseName;
    private final String claimantName;
    private final String primaryDefendantName;
    private final String nextStepUrl;

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
            "toLineClaimantName", toLineClaimantName,
            "caseNumber", caseNumber,
            "caseName", caseName,
            "claimantName", claimantName,
            "primaryDefendantName", primaryDefendantName,
            "nextStepUrl", nextStepUrl
        );
    }
}
