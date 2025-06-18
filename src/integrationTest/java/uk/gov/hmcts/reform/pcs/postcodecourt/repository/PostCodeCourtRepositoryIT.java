package uk.gov.hmcts.reform.pcs.postcodecourt.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pcs.audit.Audit;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtKey;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
public class PostCodeCourtRepositoryIT extends AbstractPostgresContainerIT {

    @Autowired
    private PostCodeCourtRepository repository;

    private final LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/London"));

    @Test
    @DisplayName("Should return only active postcode court mapping when multiple entries exist")
    void shouldReturnOnlyActivePostcodeCourtMapping() {

        String postcode = "PES123";
        int activeEpimID = 111;
        PostCodeCourtEntity activeEntity = createTestEntity(postcode, activeEpimID, currentDate, true);
        PostCodeCourtEntity nonActiveEntity = createTestEntity(postcode, 222, currentDate, false);

        repository.saveAll(List.of(activeEntity, nonActiveEntity));

        List<PostCodeCourtEntity> results = repository.findByIdPostCodeIn(List.of(postcode), currentDate);

        assertThat(results)
                .hasSize(1);
        assertThat(results.getFirst().getId().getEpimId()).isEqualTo(activeEpimID);
    }

    @Test
    @DisplayName("Should return active postcode court mapping when effectiveTo date is null")
    void shouldReturnActivePostcodeCourtMappingWhenEffectiveToDateIsNull() {

        String postcode = "PES123";
        int activeEpimID = 111;
        PostCodeCourtEntity activeEntity = createTestEntity(postcode, activeEpimID, currentDate, true);
        activeEntity.setEffectiveTo(null);

        repository.saveAll(List.of(activeEntity));

        List<PostCodeCourtEntity> results = repository.findByIdPostCodeIn(List.of(postcode), currentDate);

        assertThat(results)
                .hasSize(1);
        assertThat(results.getFirst().getId().getEpimId()).isEqualTo(activeEpimID);
    }

    @Test
    @DisplayName("Should return empty list when postcode court mapping has no active entries")
    void shouldReturnEmptyListWhenPostcodeCourtMappingHasNoActiveEntries() {
        String postcode = "V5S9H";
        PostCodeCourtEntity inactiveEntity = createTestEntity(postcode, 345, currentDate, false);

        repository.saveAll(List.of(inactiveEntity));

        List<PostCodeCourtEntity> results = repository.findByIdPostCodeIn(List.of(postcode), currentDate);

        assertThat(results).isEmpty();
    }

    private PostCodeCourtEntity createTestEntity(String postcode, int epimId, LocalDate date, boolean isActive) {
        LocalDate from = date.minusDays(4);
        LocalDate to = isActive ? date.plusDays(6) : date.minusDays(2);

        PostCodeCourtEntity entity = new PostCodeCourtEntity();
        entity.setId(new PostCodeCourtKey(postcode, epimId));
        entity.setLegislativeCountry("England");
        entity.setEffectiveFrom(from);
        entity.setEffectiveTo(to);
        entity.setAudit(createTestAudit());
        return entity;
    }

    private Audit createTestAudit() {
        Audit audit = new Audit();
        audit.setCreatedBy("test-user");
        audit.setChangeReason("test-reason");
        audit.setStatus("ACTIVE");
        return audit;
    }

}
