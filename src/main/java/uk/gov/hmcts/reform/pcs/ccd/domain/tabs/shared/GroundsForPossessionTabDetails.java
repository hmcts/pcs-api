package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroundsForPossessionTabDetails {

    @CCD(label = "Grounds", typeOverride = TextArea)
    private String grounds;

    @CCD(label = "Description of other grounds")
    private String otherGroundsDescription;
}
