package uk.gov.hmcts.reform.pcs.postalcode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.postalcode.domain.Postcode;

import java.util.Optional;

@Repository
public interface PostalCodeRepository extends JpaRepository<Postcode, Integer> {

    Optional<Postcode> findByPostcode(String postcode);

}
