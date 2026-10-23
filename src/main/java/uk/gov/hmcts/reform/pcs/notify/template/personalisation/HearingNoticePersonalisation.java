package uk.gov.hmcts.reform.pcs.notify.template.personalisation;

import lombok.Builder;

import java.util.Map;

@Builder
public class HearingNoticePersonalisation implements TemplatePersonalisation {

    private final String loginUrl;

    @Override
    public Map<String, Object> toMap() {
        return Map.of("loginUrl", loginUrl);
    }
}
