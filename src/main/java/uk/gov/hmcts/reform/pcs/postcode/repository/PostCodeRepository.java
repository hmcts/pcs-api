package uk.gov.hmcts.reform.pcs.postcode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.postcode.domain.PostCode;

import java.util.Optional;

@Repository
public interface PostCodeRepository extends JpaRepository<PostCode, Integer> {

    Optional<PostCode> findByPostCode(String postCode);

}
