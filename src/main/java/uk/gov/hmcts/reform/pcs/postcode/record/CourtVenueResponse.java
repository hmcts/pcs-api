package uk.gov.hmcts.reform.pcs.postcode.record;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtVenueResponse(
    @JsonProperty("court_venue_id") Integer courtVenueId,
    @JsonProperty("court_name") String courtName
) {}
