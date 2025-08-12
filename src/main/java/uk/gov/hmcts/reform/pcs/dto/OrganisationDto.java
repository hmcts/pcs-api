package uk.gov.hmcts.reform.pcs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

/**
 * Represent the organisation the user belongs to.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganisationDto {

    private String organisationIdentifier;
    private String name;
    private String status;
    private boolean sraRegulated;
    private SuperUserDto superUserDto;
    private List<String> paymentAccount;
    private List<AddressUK> contactInformation;
}

