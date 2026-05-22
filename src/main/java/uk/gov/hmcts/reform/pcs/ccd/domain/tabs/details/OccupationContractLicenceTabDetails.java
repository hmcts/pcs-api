package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupationContractLicenceTabDetails {

    @CCD(label = "Occupation contract or licence agreement type")
    private String agreementType;

    @CCD(label = "Occupation contract or licence start date")
    private String startDate;

    @CCD(label = "Occupation contract or licence agreement")
    private List<ListValue<Document>> documents;

}
