package uk.gov.hmcts.reform.pcs.postcode.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postcode.repository.PostCodeRepository;

@Service
@AllArgsConstructor
public class PostCodeService {

    private final PostCodeRepository postCodeRepository;

    public PostCodeResponse getEpimIdByPostCode(String postcode) {
        return postCodeRepository.findByPostCode(postcode)
                .map(pc -> new PostCodeResponse(pc.getEpimId())).orElseGet(PostCodeResponse::new);
    }

}
