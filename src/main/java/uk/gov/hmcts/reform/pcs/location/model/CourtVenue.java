package uk.gov.hmcts.reform.pcs.location.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtVenue(@JsonProperty("epimms_id") Integer epimmsId,
                         @JsonProperty("court_venue_id") Integer courtVenueId,
                         @JsonProperty("court_name")  String courtName) {
}
