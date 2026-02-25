package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantOfRestitutionDetails {

    @CCD(
        label = "How did the defendants return to the property?",
        hint = "You can upload your evidence on the next page, for example a photograph. You can enter up to 6,800 "
            + "characters",
        typeOverride = FieldType.TextArea
    )
    private String howDefendantsReturned;
}
