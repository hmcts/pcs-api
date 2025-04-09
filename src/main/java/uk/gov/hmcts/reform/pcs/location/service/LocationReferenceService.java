package uk.gov.hmcts.reform.pcs.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Service
@Slf4j
public class LocationReferenceService {

    private static final int COUNTY_COURT_TYPE_ID = 10;
    private final LocationReferenceApi locationReferenceApi;
    private final AuthTokenGenerator authTokenGenerator;


    public List<CourtVenue> getCountyCourts(@RequestHeader(AUTHORIZATION) String authorisation, Integer epimmsId) {
        log.info(String.format("Getting County courts for Epimms Id %d with the authToken size %s ", epimmsId, authorisation.length()));
        String serviceAuthorization = authTokenGenerator.generate();
        log.info(String.format("Generated service auth token size %s ", serviceAuthorization.length()));
        return locationReferenceApi.getCountyCourts(authorisation, serviceAuthorization, epimmsId, COUNTY_COURT_TYPE_ID);
    }
}
