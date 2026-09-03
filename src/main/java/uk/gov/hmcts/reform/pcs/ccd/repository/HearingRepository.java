package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface HearingRepository extends JpaRepository<HearingEntity, Integer> {

    @Query("""
        select hearing
        from HearingEntity hearing
        where hearing.pcsCase.caseReference = :caseReference
          and hearing.hearingDate >= :start
          and hearing.hearingDate < :end
          and (hearing.cancelled is null or hearing.cancelled = false)
        order by hearing.hearingDate, hearing.id
        """)
    List<HearingEntity> findActiveHearingsBetween(@Param("caseReference") long caseReference,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}
