package uk.gov.hmcts.reform.pcs.notify.template.personalisation.enforcement;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.TemplatePersonalisation;

import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
public class EnforcementDecisionPersonalisation implements TemplatePersonalisation {

    private final TemplatePersonalisation base;
    private final String caseOverviewUrl;
    private final String rejectionDetails;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());

        map.put("caseOverviewUrl", caseOverviewUrl);
        map.put("rejectionDetails", rejectionDetails);

        return map;
    }

}
