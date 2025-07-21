package uk.gov.hmcts.reform.pcs.dto;


import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;

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
    private SuperUser superUser;
    private List<String> paymentAccount;
    private List<AddressUK> contactInformation;

    public String getOrganisationIdentifier() {
        requireNonNull(organisationIdentifier);
        return organisationIdentifier;
    }

    public List<String> getPaymentAccount() {
        return paymentAccount;
    }

    public List<AddressUK> getContactInformation() {
        return contactInformation;
    }
}

