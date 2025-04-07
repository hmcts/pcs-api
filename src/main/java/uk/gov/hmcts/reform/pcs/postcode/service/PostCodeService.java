package uk.gov.hmcts.reform.pcs.postcode.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcode.adapter.LocationReferenceDataAdapter;
import uk.gov.hmcts.reform.pcs.postcode.domain.PostCode;
import uk.gov.hmcts.reform.pcs.postcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postcode.record.CourtVenue;
import uk.gov.hmcts.reform.pcs.postcode.repository.PostCodeRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class PostCodeService {

    private final PostCodeRepository postCodeRepository;

    private final LocationReferenceDataAdapter locationReferenceDataAdapter;

    public PostCodeResponse getEpimIdByPostCode(String postcode) {
        PostCode postCodeEntity = postCodeRepository.findByPostCode(postcode).orElseThrow();

        return new PostCodeResponse(postCodeEntity.getEpimId(),
                                    locationReferenceDataAdapter.fetchCountyCourts(postCodeEntity.getEpimId())
        );
    }
}
