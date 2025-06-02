package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
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
    private final IdamService idamService;

    public List<Court> getCountyCourtsByPostCode(String postcode) {

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

        return safeGetCountyCourts(epimIds).stream()
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
        return postCodeCourtRepository.findByIdPostCode(postcode);
    }

    private List<CourtVenue> safeGetCountyCourts(List<Integer> epimIds) {
        String authorisation = idamService.getSystemUserAuthorisation();

        return Try.of(() -> locationReferenceService.getCountyCourts(authorisation, epimIds))
                .onFailure(e -> log.error("Failed to fetch court details Error {}", e.getMessage(), e))
                .getOrElse(Collections.emptyList());
    }

}
