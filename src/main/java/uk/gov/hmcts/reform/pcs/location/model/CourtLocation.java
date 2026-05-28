package uk.gov.hmcts.reform.pcs.location.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtLocation(@JsonProperty("court_location_id") String courtLocationId,
                            @JsonProperty("court_location_name") String courtLocationName,
                            @JsonProperty("epims_id") String epimsId,
                            @JsonProperty("open_for_public") String openForPublic,
                            @JsonProperty("court_location_category") String courtLocationCategory,
                            @JsonProperty("region") String region,
                            @JsonProperty("region_id") String regionId,
                            @JsonProperty("cluster_name") String clusterName,
                            @JsonProperty("cluster_id") String clusterId,
                            @JsonProperty("closed_date") String closedDate,
                            @JsonProperty("postcode") String postcode,
                            @JsonProperty("court_address") String courtAddress,
                            @JsonProperty("phone_number") String phoneNumber,
                            @JsonProperty("court_location_code") String courtLocationCode,
                            @JsonProperty("dx_address") String dxAddress,
                            @JsonProperty("welsh_court_location_name") String welshCourtLocationName,
                            @JsonProperty("welsh_court_address") String welshCourtAddress
) {
}
