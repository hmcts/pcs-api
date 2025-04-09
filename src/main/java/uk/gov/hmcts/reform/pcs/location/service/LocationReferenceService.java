package uk.gov.hmcts.reform.pcs.location.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Service
public class LocationReferenceService {

    private static final int COUNTY_COURT_TYPE_ID = 10;
    private final LocationReferenceApi locationReferenceApi;
    private final AuthTokenGenerator authTokenGenerator;


    public List<CourtVenue> getCountyCourts(@RequestHeader(AUTHORIZATION) String authorisation, Integer epimmsId) {
        return locationReferenceApi.getCountyCourts(authorisation, authTokenGenerator.generate(), epimmsId,COUNTY_COURT_TYPE_ID);
    }
}
