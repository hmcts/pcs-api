package uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryLedgerEntryEntity;

import java.util.List;
import java.util.UUID;

public interface RentHistoryLedgerEntryRepository extends JpaRepository<RentHistoryLedgerEntryEntity, UUID> {

    List<RentHistoryLedgerEntryEntity> findByClaimId(UUID claimId);

}
