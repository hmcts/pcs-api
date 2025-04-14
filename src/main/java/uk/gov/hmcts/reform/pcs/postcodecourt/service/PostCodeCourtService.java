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
        List<Integer> epimmIds =  postCodeCourtRepository.findByIdPostCode(postcode).stream()
                                    .map(postCodeCourt -> postCodeCourt.getId().getEpimId())
                                    .toList();

        return safeGetCountyCourts(authorisation, epimmIds);
    }

    private List<CourtVenue> safeGetCountyCourts(String authorisation, List<Integer> epimmIds) {
        return Try.of(() -> locationReferenceService.getCountyCourts(authorisation, epimmIds))
                .onFailure(e -> log.info(String.format("Failed to fetch court details Error %s", e.getMessage())))
                .getOrElse(Collections.emptyList());
    }

}
