package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import org.apache.qpid.jms.util.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.List;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;
    private final LocationReferenceService locationReferenceService;

    public List<CourtVenue> getEpimIdByPostCode(String postcode, String authorisation) throws ResourceNotFoundException {

        int epimmsId = postCodeCourtRepository.findByPostCode(postcode)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("No matching epimmsId found for postcode %s", postcode)))
                .getEpimId();


        return locationReferenceService.getCountyCourts(authorisation, epimmsId);
    }

}
