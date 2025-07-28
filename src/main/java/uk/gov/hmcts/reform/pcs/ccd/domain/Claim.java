package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    @CCD(ignore = true)
    @JsonIgnore
    private UUID id;

    @CCD(label = "Summary")
    private String summary;

    @CCD(
        label = "Claim Amount",
        typeOverride = FieldType.MoneyGBP
    )
    private String amountInPence;

}

