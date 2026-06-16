package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;

import java.util.List;
import java.util.UUID;

public interface PartyAccessCodeRepository extends JpaRepository<PartyAccessCodeEntity, UUID> {

    List<PartyAccessCodeEntity> findAllByPcsCase_Id(UUID pcsCaseId);
}
