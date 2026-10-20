package uk.gov.hmcts.reform.pcs.ccd.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;

import static org.assertj.core.api.Assertions.assertThat;

@PcsCcdEventTest
@Transactional
class UserNameRepositoryIT extends AbstractPostgresContainerIT {

    @Autowired
    private UserNameRepository userNameRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(strings = {"1245", "idam-user-123", "7fb8bd47-d568-499f-a078-43afadc74921"})
    void storesAndFindsOpaqueIdamIdentifiers(String idamId) {
        UserNameEntity saved = userNameRepository.saveAndFlush(UserNameEntity.builder()
            .idamId(idamId)
            .name("A Judge")
            .build());
        entityManager.clear();

        assertThat(userNameRepository.findByIdamId(idamId)).hasValueSatisfying(found -> {
            assertThat(found.getId()).isEqualTo(saved.getId());
            assertThat(found.getIdamId()).isEqualTo(idamId);
        });
    }
}
