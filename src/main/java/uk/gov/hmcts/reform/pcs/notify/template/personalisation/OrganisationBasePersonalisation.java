package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class OrganisationBasePersonalisation implements TemplatePersonalisation {

    private final String caseNumber;
    private final String claimantName;
    private final String primaryDefendantName;
    private final String organisationName;

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
            "caseNumber", caseNumber,
            "claimantName", claimantName,
            "primaryDefendantName", primaryDefendantName,
            "organisationName", organisationName
        );
    }
}
