package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HearingEntityTest {

    private HearingEntity hearingEntity;

    @BeforeEach
    void setUp() {
        hearingEntity = new HearingEntity();
    }

    @Test
    void shouldAddParty() {
        // Given
        PartyEntity partyEntity = mock(PartyEntity.class);

        // When
        hearingEntity.addParty(partyEntity);

        // Then
        assertThat(hearingEntity.getHearingNoticeParties()).hasSize(1);
        assertThat(hearingEntity.getHearingNoticeParties().getFirst().getParty()).isEqualTo(partyEntity);
        assertThat(hearingEntity.getHearingNoticeParties().getFirst().getHearing()).isEqualTo(hearingEntity);
    }

    @Test
    void shouldHoldMrdKeysForHmc() {
        // When
        HearingEntity hearing = HearingEntity.builder()
            .hearingTypeKey("AAA3-FPH")
            .hearingChannelKey("VID")
            .build();

        // Then
        assertThat(hearing.getHearingTypeKey()).isEqualTo("AAA3-FPH");
        assertThat(hearing.getHearingChannelKey()).isEqualTo("VID");
        assertThat(hearing.getHearingPriorityKey()).isNull();
    }
}
