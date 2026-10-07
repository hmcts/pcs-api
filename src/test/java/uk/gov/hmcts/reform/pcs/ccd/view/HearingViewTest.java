package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HearingViewTest {

    private HearingView hearingView;

    @BeforeEach
    void setUp() {
        hearingView = new HearingView();
    }

    @Test
    void shouldMapHearingEntityToHearing() {
        // Given
        HearingEntity hearingEntity = HearingEntity.builder()
            .type(HearingType.OTHER)
            .otherHearingType("other type")
            .noticeWording(HearingNoticeWording.ADJ)
            .hearingDate(LocalDateTime.of(2026, 2, 1, 9, 0, 0))
            .durationDays(1)
            .durationMinutes(30)
            .durationHours(1)
            .notes("notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.NO)
            .additionalInformation("additional information")
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(hearingEntity))
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        hearingView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<Hearing>> hearings = pcsCase.getHearingList();
        assertThat(hearings).hasSize(1);

        Hearing hearing = hearings.getFirst().getValue();
        assertThat(hearing.getType()).isEqualTo(HearingType.OTHER);
        assertThat(hearing.getOtherHearingType()).isEqualTo("other type");
        assertThat(hearing.getNoticeWording()).isEqualTo(HearingNoticeWording.ADJ);
        assertThat(hearing.getDate()).isEqualTo(LocalDateTime.of(2026, 2, 1, 9, 0, 0));
        assertThat(hearing.getDurationDays()).isEqualTo(1);
        assertThat(hearing.getDurationMinutes()).isEqualTo(30);
        assertThat(hearing.getDurationHours()).isEqualTo(1);
        assertThat(hearing.getNotes()).isEqualTo("notes");
        assertThat(hearing.getIssueNotice()).isEqualTo(VerticalYesNo.YES);
        assertThat(hearing.getIsWithoutNotice()).isEqualTo(VerticalYesNo.NO);
        assertThat(hearing.getAdditionalInformation()).isEqualTo("additional information");
    }
}
