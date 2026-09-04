package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class HearingNoticePartyEntityTest {

    private HearingNoticePartyEntity hearingNoticePartyEntity;
    private HearingEntity hearingEntity;
    private PartyEntity partyEntity;

    @BeforeEach
    void setUp() {
        hearingEntity = new HearingEntity();
        partyEntity = new PartyEntity();
        hearingEntity.addParty(partyEntity);
        hearingNoticePartyEntity = hearingEntity.getHearingNoticeParties().getFirst();
    }

    @Test
    void shouldRemoveHearingAndParty() {
        // When
        hearingNoticePartyEntity.removeHearingNoticeParty();

        // Then
        assertThat(hearingNoticePartyEntity.getHearing()).isNull();
        assertThat(hearingNoticePartyEntity.getParty()).isNull();
        assertThat(hearingEntity.getHearingNoticeParties()).isEmpty();
        assertThat(partyEntity.getHearingNoticeParties()).isEmpty();
    }
}
