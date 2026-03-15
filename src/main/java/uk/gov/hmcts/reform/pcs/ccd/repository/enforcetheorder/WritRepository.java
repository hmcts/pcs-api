package uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;

import java.util.UUID;

public interface WritRepository extends JpaRepository<WritEntity, UUID> {
}
