package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyAccessCodeRepository extends JpaRepository<PartyAccessCodeEntity, UUID> {

    Optional<PartyAccessCodeEntity> findByPcsCase_IdAndCode(UUID pcsCaseId, String code);

    List<PartyAccessCodeEntity> findAllByPcsCase_Id(UUID pcsCaseId);
}
