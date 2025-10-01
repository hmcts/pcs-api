package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.hearings.model.CaseDetails;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Repository
public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {

    @Query
        (value = "SELECT * FROM ccd.case_data", nativeQuery = true)
//    List<CaseDetailsEntity> findAllNative();
    List<Map<String, Object>> findAllNative();
}

