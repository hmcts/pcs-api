package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNameRepository extends JpaRepository<UserNameEntity, Integer> {

    @Query("SELECT u FROM UserNameEntity u WHERE u.idamId = :idamId")
    Optional<UserNameEntity> findByIdamId(@Param("idamId") UUID idamId);

}
