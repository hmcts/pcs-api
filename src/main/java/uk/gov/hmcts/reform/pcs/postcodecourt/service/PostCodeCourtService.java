package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.InvalidPostCodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
@Slf4j
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;
    private final LocationReferenceService locationReferenceService;
    private final IdamService idamService;

    public List<Court> getCountyCourtsByPostCode(String postcode) {
        if (postcode == null || postcode.isBlank()) {
            throw new InvalidPostCodeException("Postcode can't be empty or null");
        }

        List<Integer> epimIds = getPostcodeCourtMappings(postcode).stream()
            .map(postCodeCourt -> {
                log.debug(
                    "Received epimId {} for postcode {}",
                    postCodeCourt.getId().getEpimId(),
                    postcode
                );
                return postCodeCourt.getId().getEpimId();
            })
            .toList();
        log.info("searching county courts with epimIDs {}", epimIds);
        return safeGetCountyCourts(epimIds).stream()
            .map(courtVenue -> new Court(
                courtVenue.courtVenueId(),
                courtVenue.courtName(),
                courtVenue.epimmsId()
            ))
            .toList();
    }

    private List<PostCodeCourtEntity> getPostcodeCourtMappings(String postcode) {

        postcode = postcode.replaceAll("\\s", "").toUpperCase(Locale.ROOT);

        List<String> postcodes = getPostCodeLookupCandidates(postcode);

        LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/London"));
        List<PostCodeCourtEntity> results = postCodeCourtRepository.findByIdPostCodeIn(postcodes, currentDate);
        if (results.isEmpty()) {
            log.warn("Postcode court mapping not found for postcode {}", postcode);
            return List.of();
        }

        String longestPostcodeMatch = results.stream()
            .map(e -> e.getId().getPostCode())
            .max(Comparator.comparingInt(String::length))
            .orElse("");
        List<PostCodeCourtEntity> filteredResults = results.stream()
            .filter(e -> e.getId().getPostCode().equals(longestPostcodeMatch))
            .toList();

        if (filteredResults.size() > 1) {
            log.error(
                "Multiple active EpimId's found for postcode:{} count:{}",
                longestPostcodeMatch,
                filteredResults.size()
            );
            return List.of();
        }
        log.info("Found court mapping of {} for postcode: {}", longestPostcodeMatch, postcode);
        return filteredResults;
    }

    private List<String> getPostCodeLookupCandidates(String postcode) {
        int maxTrim = 3;
        String partialPostcode = postcode;
        List<String> postcodes = new ArrayList<>();
        postcodes.add(postcode);
        for (int i = 0; i < maxTrim && partialPostcode.length() > 2; i++) {
            partialPostcode = partialPostcode.substring(0, partialPostcode.length() - 1);
            postcodes.add(partialPostcode);
        }
        return postcodes;
    }

    private List<CourtVenue> safeGetCountyCourts(List<Integer> epimIds) {
        String authorisation = idamService.getSystemUserAuthorisation();

        return Try.of(() -> locationReferenceService.getCountyCourts(authorisation, epimIds))
                .onFailure(e -> log.error("Failed to fetch court details Error {}", e.getMessage(), e))
                .getOrElse(Collections.emptyList());
    }

}
