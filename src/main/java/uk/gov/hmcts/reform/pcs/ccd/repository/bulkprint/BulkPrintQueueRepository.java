package uk.gov.hmcts.reform.pcs.ccd.repository.bulkprint;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint.PackSendStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint.PackType;
import uk.gov.hmcts.reform.pcs.ccd.entity.BulkPrintQueueEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.Optional;

public interface BulkPrintQueueRepository extends JpaRepository<BulkPrintQueueEntity, Integer> {

    List<BulkPrintQueueEntity> findByPcsCase(PcsCaseEntity pcsCaseEntity);

    // TODO: Is this the best return type?
    Optional<BulkPrintQueueEntity> findByPcsCaseAndPackType(PcsCaseEntity pcsCaseEntity, PackType packType);

    List<BulkPrintQueueEntity> findByPackSendStatus(PackSendStatus packSendStatus);

}
