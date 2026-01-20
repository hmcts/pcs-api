package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
// TEMPORARILY BROKEN: Changed from ALWAYS to NON_NULL to replicate CCD token validation bug
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Party {

    private String firstName;

    private String lastName;

    private String orgName;

    private VerticalYesNo nameKnown;

    private String emailAddress;

    private AddressUK address;

    private VerticalYesNo addressKnown;

    private VerticalYesNo addressSameAsProperty;

    private String phoneNumber;

}
