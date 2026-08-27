package uk.gov.hmcts.reform.pcs.reference.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import org.springframework.util.CollectionUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationDetailsResponse {

    public static final String ORGANISATION_PROFILE = "ORGANISATION_PROFILE";

    private String name;

    @JsonProperty("organisationIdentifier")
    private String organisationIdentifier;

    @JsonProperty("organisationProfileIds")
    private List<String> organisationProfileIds;

    @JsonProperty("contactInformation")
    private List<ContactInformation> contactInformation;

    private String status;

    @JsonProperty("sraRegulated")
    private Boolean sraRegulated;

    @JsonProperty("superUser")
    private SuperUser superUser;

    @JsonProperty("paymentAccount")
    private List<String> paymentAccount;

    @JsonProperty("pendingPaymentAccount")
    private List<String> pendingPaymentAccount;

    @JsonProperty("dateReceived")
    private String dateReceived;

    @JsonProperty("dateApproved")
    private String dateApproved;

    @JsonProperty("lastUpdated")
    private String lastUpdated;

    @JsonIgnore
    public String getOrgProfileId() {
        if (CollectionUtils.isEmpty(getOrganisationProfileIds())) {
            throw new IllegalArgumentException("No organisation profile id found for organisation "
                    + getOrganisationIdentifier());
        }

        List<String> orgProfileIds = getOrganisationProfileIds()
            .stream()
            .filter(organisationProfileId -> !ORGANISATION_PROFILE.equals(organisationProfileId))
            .distinct()
            .toList();

        if (orgProfileIds.isEmpty()) {
            throw new IllegalArgumentException("No valid organisation profile id found for organisation "
                                                   + getOrganisationIdentifier());
        }
        if (orgProfileIds.size() > 1) {
            throw new IllegalArgumentException(
                "Organisation " + getOrganisationIdentifier() + " carries more than one profile "
                    + orgProfileIds + "; cannot determine which access type applies");
        }
        return orgProfileIds.getFirst();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInformation {
        @JsonProperty("addressId")
        private String addressId;

        private String uprn;

        private String created;

        @JsonProperty("addressLine1")
        private String addressLine1;

        @JsonProperty("addressLine2")
        private String addressLine2;

        @JsonProperty("addressLine3")
        private String addressLine3;

        @JsonProperty("townCity")
        private String townCity;

        private String county;

        private String country;

        @JsonProperty("postCode")
        private String postCode;

        @JsonProperty("dxAddress")
        private List<String> dxAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuperUser {
        @JsonProperty("firstName")
        private String firstName;

        @JsonProperty("lastName")
        private String lastName;

        private String email;
    }
}
