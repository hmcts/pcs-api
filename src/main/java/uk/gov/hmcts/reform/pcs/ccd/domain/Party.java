package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Party {
    @CCD(label = "Party's forename")
    private String forename;
    @CCD(label = "Party's surname")
    private String surname;

    @CCD(ignore = true)
    private UUID idamId;

    @CCD(ignore = true)
    private UUID pcqId;
}
