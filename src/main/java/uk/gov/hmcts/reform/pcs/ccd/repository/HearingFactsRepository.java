package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingFactsEntity;

import java.util.List;
import java.util.UUID;

public interface HearingFactsRepository extends JpaRepository<HearingFactsEntity, Integer> {

    List<HearingFactsEntity> findByHearingPcsCaseId(UUID caseId);
}
