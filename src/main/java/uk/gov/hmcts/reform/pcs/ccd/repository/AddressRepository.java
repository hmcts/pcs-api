package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;

import java.util.UUID;

public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {

}
