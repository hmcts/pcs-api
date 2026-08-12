package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class LegalRepresentativeBasePersonalisation implements TemplatePersonalisation {

    // Notify was rejecting organisationName. Templates want these instead
    private final String firstName;
    private final String lastName;
    private final String caseNumber;
    private final String claimantName;
    private final String primaryDefendantName;

    @Override
    public Map<String, Object> toMap() {
        // TODO jordan: might want a dedicated LR template / personalisation later
        return Map.of(
            "firstName", firstName,
            "lastName", lastName,
            "caseNumber", caseNumber,
            "claimantName", claimantName,
            "primaryDefendantName", primaryDefendantName);
    }
}
