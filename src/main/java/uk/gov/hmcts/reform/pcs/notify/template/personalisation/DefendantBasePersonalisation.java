package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Builder
public class DefendantBasePersonalisation implements TemplatePersonalisation {
    protected final String firstName;
    protected final String lastName;
    protected final String caseNumber;
    protected final String claimantName;
    protected final String primaryDefendantName;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> personalisation = new HashMap<>();
        personalisation.put("firstName", Objects.toString(firstName, ""));
        personalisation.put("lastName", Objects.toString(lastName, ""));
        personalisation.put("caseNumber", Objects.toString(caseNumber, ""));
        personalisation.put("claimantName", Objects.toString(claimantName, ""));
        personalisation.put("primaryDefendantName", Objects.toString(primaryDefendantName, ""));
        return personalisation;
    }
}
