package uk.gov.hmcts.reform.pcs.postcodecourt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.PostcodeCourtMapping;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PostCodeCourtRepository extends JpaRepository<PostCodeCourtEntity, Integer> {

    @Query("""
            SELECT new uk.gov.hmcts.reform.pcs.postcodecourt.model.PostcodeCourtMapping(
                 p.id.postCode,
                 p.legislativeCountry,
                 p.id.epimsId,
                 p.effectiveFrom,
                 p.effectiveTo,
                 e.eligibleFrom
            )
            FROM PostCodeCourtEntity p LEFT JOIN CourtEligibilityEntity e ON e.epimsId = p.id.epimsId
            WHERE p.id.postCode IN :postcodes
        """)
    List<PostcodeCourtMapping> findByPostCodeIn(@Param("postcodes") List<String> postcodes);

    @Query("""
            SELECT new uk.gov.hmcts.reform.pcs.postcodecourt.model.PostcodeCourtMapping(
                 p.id.postCode,
                 p.legislativeCountry,
                 p.id.epimsId,
                 p.effectiveFrom,
                 p.effectiveTo,
                 e.eligibleFrom
            )
            FROM PostCodeCourtEntity p LEFT JOIN CourtEligibilityEntity e ON e.epimsId = p.id.epimsId
            WHERE (p.legislativeCountry = :legislativeCountry) AND (p.id.postCode IN :postcodes)
        """)
    List<PostcodeCourtMapping> findByPostCodeIn(@Param("postcodes") List<String> postcodes,
                                                @Param("legislativeCountry") LegislativeCountry legislativeCountry);

    @Query("""
            SELECT p FROM PostCodeCourtEntity p
            WHERE p.id.postCode IN :postcodes
              AND p.effectiveFrom <= :currentDate
              AND (p.effectiveTo IS NULL OR p.effectiveTo >= :currentDate)
        """)
    List<PostCodeCourtEntity> findActiveByPostCodeIn(List<String> postcodes, LocalDate currentDate);

}
