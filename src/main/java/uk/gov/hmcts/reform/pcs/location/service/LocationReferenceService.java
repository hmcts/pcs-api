package uk.gov.hmcts.reform.pcs.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Service
@Slf4j
public class LocationReferenceService {

    private static final int COUNTY_COURT_TYPE_ID = 10;
    private final LocationReferenceApi locationReferenceApi;
    private final AuthTokenGenerator authTokenGenerator;


    public List<CourtVenue> getCountyCourts(@RequestHeader(AUTHORIZATION) String authorisation, List<Integer> epimmIds) {

        String formattedEpimmsIds = formatEpimmsIds(epimmIds);
        log.info(String.format("Getting County courts for Epimms Id %s with the authToken size %s ", formattedEpimmsIds, authorisation.length()));
        String serviceAuthorization = authTokenGenerator.generate();
        log.info(String.format("Generated service auth token size %s ", serviceAuthorization.length()));
        return locationReferenceApi.getCountyCourts(authorisation, serviceAuthorization, formattedEpimmsIds, COUNTY_COURT_TYPE_ID);
    }

    private String formatEpimmsIds(List<Integer> epimmsIds) {
        if (epimmsIds == null || epimmsIds.isEmpty()) {
            return "";
        }
        return epimmsIds.size() == 1
                ? String.valueOf(epimmsIds.getFirst())
                : epimmsIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
