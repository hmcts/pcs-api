package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * The reason for a Wales ground; {@code ground} is the name of a {@code DiscretionaryGroundWales} or
 * {@code MandatoryGroundWales} constant.
 */
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimWalesGroundReason {

    private String ground;

    @CCD(typeOverride = TextArea)
    private String reason;

}
