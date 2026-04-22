package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagsEntity;

import java.util.UUID;

public interface RefDataFlagsRepository extends JpaRepository<RefDataFlagsEntity, UUID> {

    RefDataFlagsEntity findByFlagCode(String flagCode);
}
