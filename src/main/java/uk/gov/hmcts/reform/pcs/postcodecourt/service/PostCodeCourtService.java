package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;

    public List<PostCodeCourt> getEpimIdByPostCode(String postcode) {
        return postCodeCourtRepository.findByIdPostCode(postcode);
    }

}
