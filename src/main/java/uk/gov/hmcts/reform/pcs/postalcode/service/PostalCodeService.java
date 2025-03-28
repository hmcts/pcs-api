package uk.gov.hmcts.reform.pcs.postalcode.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postalcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postalcode.repository.PostalCodeRepository;

@Service
@AllArgsConstructor
public class PostalCodeService {

    private final PostalCodeRepository postalCodeRepository;

    public PostCodeResponse getEPIMSIdByPostcode(String postcode) {
        return postalCodeRepository.findByPostcode(postcode)
                .map(pc -> new PostCodeResponse(pc.getEpimid())).orElseGet(PostCodeResponse::new);
    }

}
