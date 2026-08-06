package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public class NoticeOfChangeNoLongerRepresentingPersonalisation implements TemplatePersonalisation {
    private final BasePersonalisation base;
    private final String organisationName;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(base.toMap());
        map.put("organisationName", organisationName);
        return Map.copyOf(map);
    }
}
