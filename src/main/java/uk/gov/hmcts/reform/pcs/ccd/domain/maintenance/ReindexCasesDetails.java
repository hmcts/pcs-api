package uk.gov.hmcts.reform.pcs.ccd.domain.maintenance;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.SuperUserAccess;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class ReindexCasesDetails {

    @CCD(
        label = "Reindex cases modified since",
        access = {SuperUserAccess.class}
    )
    private LocalDate modifiedSince;

    @CCD(
        label = "Estimated matching cases count",
        access = {SuperUserAccess.class}
    )
    private Long matchingCount;

}
