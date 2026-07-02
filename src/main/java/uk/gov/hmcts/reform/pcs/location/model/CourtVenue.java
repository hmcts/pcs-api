package uk.gov.hmcts.reform.pcs.location.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtVenue(
    @JsonProperty("court_venue_id") String courtVenueId,
    @JsonProperty("epimms_id") String epimmsId,
    @JsonProperty("site_name") String siteName,
    @JsonProperty("region_id") String regionId,
    @JsonProperty("region") String region,
    @JsonProperty("court_type") String courtType,
    @JsonProperty("court_type_id") String courtTypeId,
    @JsonProperty("cluster_id") String clusterId,
    @JsonProperty("cluster_name") String clusterName,
    @JsonProperty("open_for_public") String openForPublic,
    @JsonProperty("court_address") String courtAddress,
    @JsonProperty("postcode") String postcode,
    @JsonProperty("phone_number") String phoneNumber,
    @JsonProperty("closed_date") String closedDate,
    @JsonProperty("court_location_code") String courtLocationCode,
    @JsonProperty("dx_address") String dxAddress,
    @JsonProperty("welsh_site_name") String welshSiteName,
    @JsonProperty("welsh_court_address") String welshCourtAddress,
    @JsonProperty("court_status") String courtStatus,
    @JsonProperty("court_open_date") String courtOpenDate,
    @JsonProperty("court_name") String courtName,
    @JsonProperty("venue_name") String venueName,
    @JsonProperty("is_case_management_location") String isCaseManagementLocation,
    @JsonProperty("is_hearing_location") String isHearingLocation,
    @JsonProperty("welsh_venue_name") String welshVenueName,
    @JsonProperty("is_temporary_location") String isTemporaryLocation,
    @JsonProperty("is_nightingale_court") String isNightingaleCourt,
    @JsonProperty("location_type") String locationType,
    @JsonProperty("parent_location") String parentLocation,
    @JsonProperty("welsh_court_name") String welshCourtName,
    @JsonProperty("uprn") String uprn,
    @JsonProperty("venue_ou_code") String venueOuCode,
    @JsonProperty("mrd_building_location_id") String mrdBuildingLocationId,
    @JsonProperty("mrd_venue_id") String mrdVenueId,
    @JsonProperty("service_url") String serviceUrl,
    @JsonProperty("fact_url") String factUrl,
    @JsonProperty("external_short_name") String externalShortName,
    @JsonProperty("welsh_external_short_name") String welshExternalShortName
) {

}
