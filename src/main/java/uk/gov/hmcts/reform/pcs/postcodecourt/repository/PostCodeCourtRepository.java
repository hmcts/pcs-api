package uk.gov.hmcts.reform.pcs.postcodecourt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PostCodeCourtRepository extends JpaRepository<PostCodeCourtEntity, Integer> {

    @Query("""
            SELECT p FROM PostCodeCourtEntity p
            WHERE p.id.postCode IN :postcodes
              AND p.effectiveFrom <= :currentDate
              AND (p.effectiveTo IS NULL OR p.effectiveTo >= :currentDate)
        """)
    List<PostCodeCourtEntity> findByIdPostCodeIn(List<String> postcodes, LocalDate currentDate);

}
