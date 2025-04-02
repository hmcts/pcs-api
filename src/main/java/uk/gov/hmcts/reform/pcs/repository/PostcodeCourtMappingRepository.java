package uk.gov.hmcts.reform.pcs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pcs.entity.PostcodeCourtMapping;
import uk.gov.hmcts.reform.pcs.entity.PostcodeEpimId;

public interface PostcodeCourtMappingRepository extends JpaRepository<PostcodeCourtMapping, PostcodeEpimId> {
}
