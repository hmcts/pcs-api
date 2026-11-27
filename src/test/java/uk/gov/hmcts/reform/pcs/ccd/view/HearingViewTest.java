package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureFlags;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

public class HearingViewTest {

    private HearingView hearingView;

    @BeforeEach
    void setUp() {
        hearingView = new HearingView(Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), UK_ZONE_ID));
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

    @Test
    void shouldShowExistingHearingsInDateOrderWithTheirStatus() {
        PCSCase pcsCase = PCSCase.builder()
            .featureFlags(FeatureFlags.builder().makeOrderEnabled(VerticalYesNo.YES).build())
            .build();
        HearingEntity past = HearingEntity.builder()
            .type(HearingType.OTHER)
            .otherHearingType("Review <with directions>")
            .hearingDate(LocalDateTime.of(2026, 11, 25, 9, 0))
            .build();
        HearingEntity scheduled = HearingEntity.builder()
            .type(HearingType.OTHER)
            .otherHearingType("First listing")
            .hearingDate(LocalDateTime.of(2026, 11, 30, 11, 0))
            .build();
        HearingEntity cancelled = HearingEntity.builder()
            .type(HearingType.OTHER)
            .otherHearingType("Relisted case")
            .hearingDate(LocalDateTime.of(2026, 12, 1, 13, 30))
            .cancelled(true)
            .build();

        hearingView.setCaseFields(pcsCase, PcsCaseEntity.builder()
            .hearings(List.of(cancelled, past, scheduled)).build());

        assertThat(pcsCase.getHearingListMarkdown())
            .containsSubsequence("25 November 2026", "Review &lt;with directions&gt;", ">Past<",
                "30 November 2026", "First listing", ">Scheduled<",
                "1 December 2026", "Relisted case", ">Cancelled<");
    }

    @Test
    void shouldShowAnEmptyStateWithTheReleaseFlagOnAndClearItWhenOff() {
        PCSCase pcsCase = PCSCase.builder()
            .featureFlags(FeatureFlags.builder().makeOrderEnabled(VerticalYesNo.YES).build())
            .build();
        PcsCaseEntity caseWithoutHearings = PcsCaseEntity.builder().build();

        hearingView.setCaseFields(pcsCase, caseWithoutHearings);
        assertThat(pcsCase.getHearingListMarkdown()).isEqualTo("There are no hearings recorded on this case.");

        pcsCase.getFeatureFlags().setMakeOrderEnabled(VerticalYesNo.NO);
        hearingView.setCaseFields(pcsCase, caseWithoutHearings);
        assertThat(pcsCase.getHearingListMarkdown()).isNull();
    }
}
