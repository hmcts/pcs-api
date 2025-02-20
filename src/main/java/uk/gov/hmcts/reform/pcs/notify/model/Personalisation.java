package uk.gov.hmcts.reform.pcs.notify.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class Personalisation {
    private Map<String, Object> personalisation;
}
