package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class ClaimantBasePersonalisation implements TemplatePersonalisation {
    private final String toLineClaimantName;
    private final String caseNumber;
    private final String claimantName;
    private final String primaryDefendantName;

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
            "toLineClaimantName", toLineClaimantName,
            "caseNumber", caseNumber,
            "claimantName", claimantName,
            "primaryDefendantName", primaryDefendantName
        );
    }
}
