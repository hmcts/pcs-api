package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class DefendantBasePersonalisation implements TemplatePersonalisation {
    protected final String firstName;
    protected final String lastName;
    protected final String caseNumber;
    protected final String claimantName;
    protected final String primaryDefendantName;

    @Override
    public Map<String, Object> toMap() {
        return BasePersonalisation.buildBaseMap(firstName, lastName, caseNumber, claimantName, primaryDefendantName);
    }
}
