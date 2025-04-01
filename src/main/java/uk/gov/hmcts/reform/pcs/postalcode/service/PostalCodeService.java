package uk.gov.hmcts.reform.pcs.postalcode.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postalcode.repository.PostalCodeRepository;

@Service
@AllArgsConstructor
public class PostalCodeService {

    private final PostalCodeRepository postalCodeRepository;

    public PostCodeResponse getEpimIdByPostCode(String postcode) {
        return postalCodeRepository.findByPostCode(postcode)
                .map(pc -> new PostCodeResponse(pc.getEpimId())).orElseGet(PostCodeResponse::new);
    }

}
