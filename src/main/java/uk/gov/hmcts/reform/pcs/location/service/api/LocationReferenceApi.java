package uk.gov.hmcts.reform.pcs.location.service.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.pcs.location.model.CourtLocation;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(name = "location-reference", url = "${location-reference.api-url}")
public interface LocationReferenceApi {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    String COURT_VENUES_ENDPOINT = "/refdata/location/court-venues";

    String COURT_LOCATIONS_ENDPOINT = "/refdata/location/court-locations";

    @GetMapping(COURT_VENUES_ENDPOINT)
    List<CourtVenue> getCountyCourts(
        @RequestHeader(AUTHORIZATION) String authorisation,
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
        @RequestParam(name = "epimms_id", required = false) String epimmsId,
        @RequestParam(name = "court_type_id", required = false) Integer courTypeId
    );

    @GetMapping(COURT_LOCATIONS_ENDPOINT)
    List<CourtLocation> getCourtLocations(@RequestHeader(AUTHORIZATION) String authorisation,
                                          @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorization,
                                          @RequestParam(name = "epimms_id", required = false) String epimmsId);

}
