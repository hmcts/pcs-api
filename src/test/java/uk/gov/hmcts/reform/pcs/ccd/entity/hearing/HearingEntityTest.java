package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HearingEntityTest {

    private HearingEntity hearingEntity;

    @BeforeEach
    void setUp() {
        hearingEntity = new HearingEntity();
    }

    @Test
    void shouldAddParty() {
        // Given
        UUID uuid = mock(UUID.class);

        // When
        hearingEntity.addParty(uuid);

        // Then
        assertThat(hearingEntity.getNoticeParties()).hasSize(1);
        assertThat(hearingEntity.getNoticeParties().getFirst()).isEqualTo(uuid);
    }
}
