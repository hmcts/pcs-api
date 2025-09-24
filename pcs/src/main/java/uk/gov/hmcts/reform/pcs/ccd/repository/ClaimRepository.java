package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.util.UUID;


@Repository
public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {

}
