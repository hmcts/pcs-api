package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoDateOfEviction;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceOfDefendants;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DateOfEviction {

    @CCD(
        label = "Are there any other dates in the next 3 months that you (or someone from your organisation) " +
            "cannot attend an eviction?"
    )
    private YesNoDateOfEviction dateOfEvictions;

    @CCD(label = "None availability", hint = "Please add dates")
    private List<ListValue<CapturingDob>> additionalDates;

}
