package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.UUID;

@Repository
public interface GenAppRepository extends JpaRepository<GenAppEntity, UUID> {

    boolean existsByPcsCaseAndClientReference(PcsCaseEntity pcsCaseEntity, String clientReference);

}
