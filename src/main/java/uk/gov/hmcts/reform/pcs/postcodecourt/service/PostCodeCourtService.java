package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;
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

    public List<CourtVenue> getEpimIdByPostCode(String postcode, String authorisation)  {
        return postCodeCourtRepository.findByIdPostCode(postcode).stream()
                .map(postCodeCourt -> postCodeCourt.getId().getEpimId())
                .map(epimmsId -> safeGetCountyCourts(authorisation, epimmsId))
                .flatMap(List::stream)
                .toList();
    }

    private List<CourtVenue> safeGetCountyCourts(String authorisation, Integer epimmsId) {
        return Try.of(() -> locationReferenceService.getCountyCourts(authorisation, epimmsId))
                .onFailure(e -> log.info(String.format("Failed to fetch courts for epimmsId: {%d} Error [%s]", epimmsId, e.getMessage())))
                .getOrElse(Collections.emptyList());
    }

}
