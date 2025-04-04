package uk.gov.hmcts.reform.pcs.postcode.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcode.domain.PostCode;
import uk.gov.hmcts.reform.pcs.postcode.dto.PostCodeResponse;
import uk.gov.hmcts.reform.pcs.postcode.repository.PostCodeRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
public class PostCodeService {

    private final PostCodeRepository postCodeRepository;

    public PostCodeResponse getEpimIdByPostCode(String postcode) {
        PostCode postCodeEntity = postCodeRepository.findByPostCode(postcode).orElseThrow();

        postCodeEntity.getEpimId();


        return postCodeOptional
                .map(pc -> new PostCodeResponse(pc.getEpimId())).orElseGet(PostCodeResponse::new);
    }

}
