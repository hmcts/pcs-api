package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class BasePersonalisation implements TemplatePersonalisation {
    protected final String firstName;
    protected final String lastName;
    protected final String caseNumber;
    protected final String claimantName;
    protected final String primaryDefendantName;

    @Override
    public Map<String, Object> toMap() {
        return Map.of(
            "firstName", firstName,
            "lastName", lastName,
            "caseNumber", caseNumber,
            "claimantName", claimantName,
            "primaryDefendantName", primaryDefendantName
        );
    }
}
