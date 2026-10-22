package uk.gov.hmcts.reform.pcs.enforcement.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BailiffAllocationEntityTest {

    @Test
    void shouldBeActiveWhileItHasNotEnded() {
        BailiffAllocationEntity allocation = BailiffAllocationEntity.builder()
            .startedAt(Instant.parse("2026-10-01T09:00:00Z"))
            .build();

        assertThat(allocation.isActive()).isTrue();
    }

    @Test
    void shouldNotBeActiveOnceItHasEnded() {
        BailiffAllocationEntity allocation = BailiffAllocationEntity.builder()
            .startedAt(Instant.parse("2026-10-01T09:00:00Z"))
            .endedAt(Instant.parse("2026-10-02T09:00:00Z"))
            .build();

        assertThat(allocation.isActive()).isFalse();
    }
}
