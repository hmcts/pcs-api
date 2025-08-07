package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {
    @CCD(label = "Party's forename")
    private String forename;

    @CCD(label = "Party's surname")
    private String surname;

    @CCD(typeOverride = FieldType.Text, showCondition = NEVER_SHOW)
    private UUID idamId;

    @CCD(typeOverride = FieldType.Text, showCondition = NEVER_SHOW)
    private UUID pcqId;

}
