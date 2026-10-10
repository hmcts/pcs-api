package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Builder
public class BasePersonalisation implements TemplatePersonalisation {
    protected final String firstName;
    protected final String lastName;
    protected final String caseNumber;
    protected final String claimantName;
    protected final String primaryDefendantName;

    @Override
    public Map<String, Object> toMap() {
        return buildBaseMap(firstName, lastName, caseNumber, claimantName, primaryDefendantName);
    }

    static Map<String, Object> buildBaseMap(
        String firstName,
        String lastName,
        String caseNumber,
        String claimantName,
        String primaryDefendantName
    ) {
        Map<String, Object> personalisation = new HashMap<>();
        personalisation.put("firstName", Objects.toString(firstName, ""));
        personalisation.put("lastName", Objects.toString(lastName, ""));
        personalisation.put("caseNumber", Objects.requireNonNull(caseNumber, "caseNumber must not be null"));
        personalisation.put("claimantName", Objects.requireNonNull(claimantName, "claimantName must not be null"));
        personalisation.put("primaryDefendantName",
            Objects.requireNonNull(primaryDefendantName, "primaryDefendantName must not be null"));
        return personalisation;
    }
}
