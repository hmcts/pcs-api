package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;

    public List<PostCodeCourt> getEpimIdByPostCode(String postcode) {
        List<PostCodeCourt> byIdPostCode = postCodeCourtRepository.findByIdPostCode(postcode);
        log.debug("Found {} courts for postcode {}", byIdPostCode.size(), postcode);
        return byIdPostCode;
    }

}
