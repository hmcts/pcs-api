package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;

import java.util.List;

public interface HearingRepository extends JpaRepository<HearingEntity, Integer> {

    @Query(value = """
        SELECT key AS code, value_en AS label
        FROM public.case_management_cancellation_reason
        WHERE active = 'YES'
        ORDER BY value_en, key
        """, nativeQuery = true)
    List<CancellationReason> findActiveCancellationReasons();

    interface CancellationReason {
        String getCode();

        String getLabel();
    }
}
