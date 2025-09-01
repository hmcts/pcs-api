package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import java.util.UUID;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.Label;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasonForGrounds {
    @CCD(
        typeOverride = Label
    )
    private String name;

    @CCD(
        label = "Why are you making a claim for possession under this ground?",
        typeOverride = TextArea
    )
    private String value;

}

