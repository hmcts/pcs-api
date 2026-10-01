package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint.PackSendStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.bulkprint.PackType;
import uk.gov.hmcts.reform.pcs.ccd.entity.BulkPrintQueueEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.bulkprint.BulkPrintQueueRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BulkPrintQueueService {

    private final BulkPrintQueueRepository bulkPrintQueueRepository;

    public void queueClaimPack(PcsCaseEntity pcsCaseEntity, ClaimEntity claimEntity) {
        BulkPrintQueueEntity bulkPrintQueueEntity = BulkPrintQueueEntity.builder()
            .pcsCase(pcsCaseEntity)
            .packType(PackType.CLAIM)
            .claim(claimEntity)
            .packSendStatus(PackSendStatus.PENDING)
            .build();

        // TODO: Upsert if a row already exists?
        bulkPrintQueueRepository.save(bulkPrintQueueEntity);
    }

    public void queueGenAppPack(PcsCaseEntity pcsCaseEntity, GenAppEntity genAppEntity) {
        BulkPrintQueueEntity bulkPrintQueueEntity = BulkPrintQueueEntity.builder()
            .pcsCase(pcsCaseEntity)
            .packType(PackType.GEN_APP)
            .genAppEntity(genAppEntity)
            .packSendStatus(PackSendStatus.PENDING)
            .build();

        // TODO: Upsert if a row already exists?
        bulkPrintQueueRepository.save(bulkPrintQueueEntity);
    }

    public List<BulkPrintQueueEntity> getQueuedItemsForCase(PcsCaseEntity pcsCaseEntity) {
        return bulkPrintQueueRepository.findByPcsCase(pcsCaseEntity);
    }

    public void registerSentClaimPack(PcsCaseEntity pcsCaseEntity) {
        BulkPrintQueueEntity queueEntity = bulkPrintQueueRepository.findByPcsCaseAndPackType(
                pcsCaseEntity,
                PackType.CLAIM
            )
            .orElseThrow(() -> new RuntimeException("No queue entry found"));

        queueEntity.setSentAt(LocalDateTime.now());
        queueEntity.setPackSendStatus(PackSendStatus.SENT);
        bulkPrintQueueRepository.save(queueEntity);
    }

    public List<UUID> getCaseIdsPendingPacks() {
        // TODO: Update query to just fetch IDs?
        return bulkPrintQueueRepository.findByPackSendStatus(PackSendStatus.PENDING).stream()
            .map(queueEntity -> queueEntity.getPcsCase().getId())
            .toList();
    }
}
