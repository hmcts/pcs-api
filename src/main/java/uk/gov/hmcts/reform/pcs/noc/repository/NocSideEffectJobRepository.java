package uk.gov.hmcts.reform.pcs.noc.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobEntity;

public interface NocSideEffectJobRepository extends JpaRepository<NocSideEffectJobEntity, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
