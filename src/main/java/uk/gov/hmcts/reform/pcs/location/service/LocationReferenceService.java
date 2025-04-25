package uk.gov.hmcts.reform.pcs.location.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${location-reference.court-county-type-id:10}")
    @Getter
    private Integer countyCourtTypeId;

    private final LocationReferenceApi locationReferenceApi;
    private final AuthTokenGenerator authTokenGenerator;


    public List<CourtVenue> getCountyCourts(String authorisation, List<Integer> epimIds) {
        if (Objects.isNull(epimIds) || epimIds.isEmpty()) {
            throw new IllegalArgumentException("epimIds cannot be null or empty");
        }
        String formattedEpimIds = formatEpimIds(epimIds);
        log.debug("Getting County courts from /refdata/location/court-venues for EpimIds {}", formattedEpimIds);
        return locationReferenceApi.getCountyCourts(authorisation, authTokenGenerator.generate(),
                formattedEpimIds, countyCourtTypeId);
    }

    private String formatEpimIds(List<Integer> epimIds) {
        if (epimIds.size() > 1) {
            log.warn("Received multiple epimIds: {}", epimIds);
        }

        return epimIds.size() == 1
                ? String.valueOf(epimIds.getFirst())
                : epimIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
