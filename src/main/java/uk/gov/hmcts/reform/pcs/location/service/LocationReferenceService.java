package uk.gov.hmcts.reform.pcs.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class LocationReferenceService {

    private static final int COUNTY_COURT_TYPE_ID = 10;
    private final LocationReferenceApi locationReferenceApi;
    private final AuthTokenGenerator authTokenGenerator;

    public List<CourtVenue> getCountyCourts(String authorisation, List<Integer> epimIds) {
        if (Objects.isNull(epimIds) || epimIds.isEmpty()) {
            throw new IllegalArgumentException("epimIds cannot be null or empty");
        }
        String formattedEpimIds = formatEpimIds(epimIds);
        log.info("Getting County courts for EpimIds {}", formattedEpimIds);
        String serviceAuthorization = authTokenGenerator.generate();
        return locationReferenceApi.getCountyCourts(authorisation, serviceAuthorization,
                formattedEpimIds, COUNTY_COURT_TYPE_ID);
    }

    private String formatEpimIds(List<Integer> epimIds) {
        return epimIds.size() == 1
                ? String.valueOf(epimIds.getFirst())
                : epimIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
