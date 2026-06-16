package uk.gov.hmcts.reform.pcs.location.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Court venue from {@code rd-location-ref-api}'s {@code /refdata/location/court-venues} response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CourtVenue(@JsonProperty("epimms_id") Integer epimmsId,
                         @JsonProperty("court_venue_id") Integer courtVenueId,
                         @JsonProperty("court_name") String courtName,
                         @JsonProperty("court_address") String courtAddress,
                         @JsonProperty("postcode") String postcode) {
}
