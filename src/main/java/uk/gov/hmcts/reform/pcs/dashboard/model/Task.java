package uk.gov.hmcts.reform.pcs.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.util.Map;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String templateId;
    @CCD(typeOverride = TextArea)
    private Map<String, Object> templateValues;
    private String status;
}
