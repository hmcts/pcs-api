package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;

    public Optional<String> getEpimIdByPostCode(String postcode) {
        return postCodeCourtRepository.findByPostCode(postcode).map(postCode -> String.valueOf(postCode.getEpimId()));
    }

}
