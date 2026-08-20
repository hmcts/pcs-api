package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class OrganisationBasePersonalisation implements TemplatePersonalisation {

    private final String organisationName;
    private final String caseNumber;
    private final String claimantName;
    private final String primaryDefendantName;

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
            "organisationName", organisationName,
            "caseNumber", caseNumber,
            "claimantName", claimantName,
            "primaryDefendantName", primaryDefendantName);
    }
}
