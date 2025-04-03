package uk.gov.hmcts.reform.pcs.postcodecourt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.postcodecourt.domain.PostCodeCourt;

import java.util.Optional;

@Repository
public interface PostCodeCourtRepository extends JpaRepository<PostCodeCourt, Integer> {

    Optional<PostCodeCourt> findByPostCode(String postCode);

}
