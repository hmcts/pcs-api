package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class WritDetails {

    @JsonUnwrapped
    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

    @CCD(
            searchable = false
    )
    private VerticalYesNo showChangeNameAddressPage;

    @CCD(
            searchable = false
    )
    private VerticalYesNo showPeopleWhoWillBeEvictedPage;
}
