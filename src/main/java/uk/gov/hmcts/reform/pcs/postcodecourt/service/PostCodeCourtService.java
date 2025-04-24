package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.Court;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.Collections;
import java.util.List;

import io.vavr.control.Try;

@Service
@AllArgsConstructor
@Slf4j
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;
    private final LocationReferenceService locationReferenceService;

    public List<Court> getCountyCourtsByPostCode(String postcode, String authorisation)  {
        List<Integer> epimIds = postCodeCourtRepository.findByIdPostCode(postcode).stream()
            .map(postCodeCourt -> {
                log.info(
                    "Received epimId {} for postcode {}",
                    postCodeCourt.getId().getEpimId(),
                    postcode
                );
                return postCodeCourt.getId().getEpimId();
            })
            .toList();
        return safeGetCountyCourts(authorisation, epimIds).stream()
            .map(courtVenue -> new Court(
                courtVenue.courtVenueId(),
                courtVenue.courtName(),
                courtVenue.epimmsId()
            ))
            .toList();
    }

    private List<CourtVenue> safeGetCountyCourts(String authorisation, List<Integer> epimIds) {
        return Try.of(() -> locationReferenceService.getCountyCourts(authorisation, epimIds))
                .onFailure(e -> log.error("Failed to fetch court details Error {}", e.getMessage()))
                .getOrElse(Collections.emptyList());
    }

}
