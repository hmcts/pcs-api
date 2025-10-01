package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.util.List;
import java.util.UUID;

@Builder
@Data
public class GeneralApplication {

    @CCD(ignore = true)
    @JsonIgnore
    private final UUID id;

    @CCD(label = "Details of your application", typeOverride = FieldType.TextArea)
    private String summary;

    @CCD(ignore = true)
    @JsonIgnore
    private List<Party> applicants;

}
