package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.PostCodeNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.vavr.control.Try;

@Service
@AllArgsConstructor
@Slf4j
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;
    private final LocationReferenceService locationReferenceService;

    public List<Court> getCountyCourtsByPostCode(String postcode, String authorisation) {

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
        if (postcode == null) {
            log.warn("Returning empty list of postcode court mappings for null postcode.");
            return List.of();
        }
        postcode = postcode.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        List<PostCodeCourtEntity> courts = postCodeCourtRepository.findByIdPostCode(postcode);

        if (!courts.isEmpty()) {
            return courts;
        }
        //Trimming post code for partial match
        String trimmedPostCode = postcode;
        int maxTrim = 3;
        for (int i = 0; i < maxTrim; i++) {
            log.info("Trimming last character of postcode: {} for partial match", trimmedPostCode);
            trimmedPostCode = postcode.substring(0, trimmedPostCode.length() - 1);
            courts = postCodeCourtRepository.findByIdPostCode(trimmedPostCode);
            if (!courts.isEmpty()) {
                log.info("Found postcode: {} for partial match", trimmedPostCode);
                return courts;
            }
        }
        //Throw exception if no full or partial match is found
        throw new PostCodeNotFoundException("No court mapping found for postcode:" + postcode);
    }

    private List<CourtVenue> safeGetCountyCourts(String authorisation, List<Integer> epimIds) {
        return Try.of(() -> locationReferenceService.getCountyCourts(authorisation, epimIds))
                .onFailure(e -> log.error("Failed to fetch court details Error {}", e.getMessage(), e))
                .getOrElse(Collections.emptyList());
    }

}
