package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalDefendantInformationTabDetails {

    @CCD(label = "Additional defendant’s name known?")
    private String nameKnown;

    @CCD(label = "First name")
    private String firstName;

    @CCD(label = "Last name")
    private String lastName;

    @CCD(label = "Additional defendant’s address for service known?")
    private String addressKnown;

    @CCD(label = "Additional defendant address for service")
    private AddressUK addressForService;
}
