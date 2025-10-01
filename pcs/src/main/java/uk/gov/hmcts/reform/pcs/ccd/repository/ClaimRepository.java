package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Repository
public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID>, ClaimRepositoryCustom {

    @Query
        (value = "SELECT * FROM ccd.case_data", nativeQuery = true)
    List<Map<String, Object>> findAllNative();
}

