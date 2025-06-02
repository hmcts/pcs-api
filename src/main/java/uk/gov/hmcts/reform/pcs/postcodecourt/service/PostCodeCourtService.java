package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.InvalidPostCodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

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

    public List<Court> getCountyCourtsByPostCode(String postcode, String authorisation) {
        if (postcode == null || postcode.isBlank()) {
            throw new InvalidPostCodeException("Postcode cannot be empty or null");
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
        return safeGetCountyCourts(authorisation, epimIds).stream()
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
        List<PostCodeCourtEntity> results = postCodeCourtRepository.findByIdPostCodeIn(postcodes);
        if (results.isEmpty()) {
            log.warn("Postcode court mapping not found for postcode {}", postcode);
            return List.of();
        }
        PostCodeCourtEntity postCodeMatch = results.stream()
            .max(Comparator.comparingInt(e -> e.getId().getPostCode().length()))
            .get();
        log.info("Found court mapping for postcode {} as {}", postcode, postCodeMatch.getId().getPostCode());
        return List.of(postCodeMatch);
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

    private List<CourtVenue> safeGetCountyCourts(String authorisation, List<Integer> epimIds) {
        return Try.of(() -> locationReferenceService.getCountyCourts(authorisation, epimIds))
                .onFailure(e -> log.error("Failed to fetch court details Error {}", e.getMessage(), e))
                .getOrElse(Collections.emptyList());
    }

}
