package uk.gov.hmcts.reform.pcs.postcodecourt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;

import java.util.List;

@Repository
public interface PostCodeCourtRepository extends JpaRepository<PostCodeCourtEntity, Integer> {

    List<PostCodeCourtEntity> findByIdPostCode(String postCode);

}
