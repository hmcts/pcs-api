package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingListingStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Test
    void shouldHoldListingStatusAndVenueWithoutBeingRequired() {
        // When
        HearingEntity hearing = HearingEntity.builder()
            .listingStatus(HearingListingStatus.FIXED)
            .hearingVenueId(7)
            .build();

        // Then
        assertThat(hearing.getListingStatus()).isEqualTo(HearingListingStatus.FIXED);
        assertThat(hearing.getHearingVenueId()).isEqualTo(7);
        assertThat(new HearingEntity().getListingStatus()).isNull();
        assertThat(new HearingEntity().getLastModifiedDate()).isNull();
    }

    @Test
    void shouldMirrorLegacyColumnsIntoModelColumns() {
        // Given
        UUID caseId = UUID.randomUUID();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(caseId).build();
        LocalDateTime hearingDate = LocalDateTime.of(2026, 11, 1, 10, 0);
        hearingEntity.setPcsCase(pcsCase);
        hearingEntity.setHearingDate(hearingDate);
        hearingEntity.setIssueNotice(VerticalYesNo.YES);

        // When
        hearingEntity.syncModelColumns();

        // Then
        assertThat(hearingEntity.getDate()).isEqualTo(hearingDate);
        assertThat(hearingEntity.getNoticeIssued()).isEqualTo(VerticalYesNo.YES);
        assertThat(hearingEntity.getPcsCaseId()).isEqualTo(caseId);
        assertThat(hearingEntity.getNoticeParties()).isNull();
    }

    @Test
    void shouldLeavePcsCaseIdEmptyWhenHearingHasNoCase() {
        // When
        hearingEntity.syncModelColumns();

        // Then
        assertThat(hearingEntity.getPcsCaseId()).isNull();
    }
}
