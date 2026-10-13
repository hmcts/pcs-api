package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingFactsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HearingFactsRepository extends JpaRepository<HearingFactsEntity, Integer> {

    /** The facts recorded at the case's latest hearing other than this one. */
    Optional<HearingFactsEntity> findFirstByHearingPcsCaseIdAndHearingIdNotOrderByHearingHearingDateDescHearingIdDesc(
        UUID caseId, Integer hearingId);

    List<HearingFactsEntity> findByHearingPcsCaseId(UUID caseId);
}
