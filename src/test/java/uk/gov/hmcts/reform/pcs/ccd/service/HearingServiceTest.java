package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.DynamicMultiSelectList;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HearingServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @InjectMocks
    private HearingService hearingService;

    @Test
    void shouldSaveHearing() {
        // Given
        Hearing hearing = Hearing.builder()
            .type(HearingType.OTHER)
            .otherHearingType("other hearing type")
            .noticeWording(HearingNoticeWording.ADJ)
            .date(LocalDateTime.of(2026, 2, 1, 9,  0, 0))
            .durationDays(1)
            .durationHours(1)
            .durationMinutes(30)
            .notes("notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .additionalInformation("additional information")
            .build();

        UUID partyId = UUID.randomUUID();
        List<DynamicListElement> listItems = List.of(
            DynamicListElement.builder()
                .code(partyId)
                .build()
        );
        DynamicMultiSelectList partyList = DynamicMultiSelectList.builder()
            .value(listItems)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .hearing(hearing)
            .partyMultiSelectionList(partyList)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // When
        hearingService.addHearing(caseReference, pcsCase);

        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getHearings()).hasSize(1);

        // Then
        HearingEntity hearingEntity = persistedCaseEntity.getHearings().getFirst();
        assertThat(hearingEntity.getType()).isEqualTo(HearingType.OTHER);
        assertThat(hearingEntity.getOtherHearingType()).isEqualTo("other hearing type");
        assertThat(hearingEntity.getNoticeWording()).isEqualTo(HearingNoticeWording.ADJ);
        assertThat(hearingEntity.getHearingDate()).isEqualTo(LocalDateTime.of(2026, 2, 1, 9, 0, 0));
        assertThat(hearingEntity.getDurationDays()).isEqualTo(1);
        assertThat(hearingEntity.getDurationHours()).isEqualTo(1);
        assertThat(hearingEntity.getDurationMinutes()).isEqualTo(30);
        assertThat(hearingEntity.getNotes()).isEqualTo("notes");
        assertThat(hearingEntity.getIssueNotice()).isEqualTo(VerticalYesNo.YES);
        assertThat(hearingEntity.getIsWithoutNotice()).isEqualTo(VerticalYesNo.YES);
        assertThat(hearingEntity.getAdditionalInformation()).isEqualTo("additional information");
        assertThat(hearingEntity.getNoticeParties()).hasSize(1);
        assertThat(hearingEntity.getNoticeParties().getFirst()).isEqualTo(partyId);
    }

    @Test
    void shouldNotSavePartiesIfIsWithoutNotice() {
        // Given
        Hearing hearing = Hearing.builder()
            .type(HearingType.OTHER)
            .otherHearingType("other hearing type")
            .noticeWording(HearingNoticeWording.ADJ)
            .date(LocalDateTime.of(2026, 2, 1, 9,  0, 0))
            .durationDays(1)
            .durationHours(1)
            .durationMinutes(30)
            .notes("notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.NO)
            .additionalInformation("additional information")
            .build();

        UUID partyId = UUID.randomUUID();
        List<DynamicListElement> listItems = List.of(
            DynamicListElement.builder()
                .code(partyId)
                .build()
        );
        DynamicMultiSelectList partyList = DynamicMultiSelectList.builder()
            .value(listItems)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .hearing(hearing)
            .partyMultiSelectionList(partyList)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // When
        hearingService.addHearing(caseReference, pcsCase);

        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getHearings()).hasSize(1);

        // Then
        HearingEntity hearingEntity = persistedCaseEntity.getHearings().getFirst();
        assertThat(hearingEntity.getType()).isEqualTo(HearingType.OTHER);
        assertThat(hearingEntity.getOtherHearingType()).isEqualTo("other hearing type");
        assertThat(hearingEntity.getNoticeWording()).isEqualTo(HearingNoticeWording.ADJ);
        assertThat(hearingEntity.getHearingDate()).isEqualTo(LocalDateTime.of(2026, 2, 1, 9, 0, 0));
        assertThat(hearingEntity.getDurationDays()).isEqualTo(1);
        assertThat(hearingEntity.getDurationHours()).isEqualTo(1);
        assertThat(hearingEntity.getDurationMinutes()).isEqualTo(30);
        assertThat(hearingEntity.getNotes()).isEqualTo("notes");
        assertThat(hearingEntity.getIssueNotice()).isEqualTo(VerticalYesNo.YES);
        assertThat(hearingEntity.getIsWithoutNotice()).isEqualTo(VerticalYesNo.NO);
        assertThat(hearingEntity.getAdditionalInformation()).isEqualTo("additional information");
        assertThat(hearingEntity.getNoticeParties()).isEmpty();
    }

    @Test
    void shouldUpdateSelectedHearingAndPreserveOtherHearings() {
        // Given
        Hearing hearing = Hearing.builder()
            .type(HearingType.OTHER)
            .otherHearingType("updated other hearing type")
            .noticeWording(HearingNoticeWording.RES)
            .date(LocalDateTime.of(2026, 3, 4, 10, 15, 0))
            .durationDays(2)
            .durationHours(3)
            .durationMinutes(45)
            .notes("updated notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .additionalInformation("updated additional information")
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicMultiSelectList partyList = DynamicMultiSelectList.builder()
            .value(List.of(DynamicListElement.builder().code(partyId).build()))
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("2")
            .hearing(hearing)
            .partyMultiSelectionList(partyList)
            .build();

        HearingEntity nonSelectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.POSSESSION)
            .hearingDate(LocalDateTime.of(2026, 2, 1, 9, 0, 0))
            .build();
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(2)
            .type(HearingType.APPLICATION)
            .otherHearingType("old other hearing type")
            .noticeWording(HearingNoticeWording.ADJ)
            .hearingDate(LocalDateTime.of(2026, 2, 2, 9, 0, 0))
            .durationDays(0)
            .durationHours(1)
            .durationMinutes(30)
            .notes("old notes")
            .issueNotice(VerticalYesNo.NO)
            .isWithoutNotice(VerticalYesNo.NO)
            .additionalInformation("old additional information")
            .noticeParties(List.of(UUID.randomUUID()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(nonSelectedHearing, selectedHearing))
            .build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // When
        hearingService.updateHearing(caseReference, pcsCase);

        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        PcsCaseEntity persistedCaseEntity = pcsCaseEntityCaptor.getValue();
        assertThat(persistedCaseEntity.getHearings()).hasSize(2);
        assertThat(persistedCaseEntity.getHearings().getFirst()).isSameAs(nonSelectedHearing);

        // Then
        HearingEntity hearingEntity = persistedCaseEntity.getHearings().getLast();
        assertThat(hearingEntity.getId()).isEqualTo(2);
        assertThat(hearingEntity.getType()).isEqualTo(HearingType.OTHER);
        assertThat(hearingEntity.getOtherHearingType()).isEqualTo("updated other hearing type");
        assertThat(hearingEntity.getNoticeWording()).isEqualTo(HearingNoticeWording.RES);
        assertThat(hearingEntity.getHearingDate()).isEqualTo(LocalDateTime.of(2026, 3, 4, 10, 15, 0));
        assertThat(hearingEntity.getDurationDays()).isEqualTo(2);
        assertThat(hearingEntity.getDurationHours()).isEqualTo(3);
        assertThat(hearingEntity.getDurationMinutes()).isEqualTo(45);
        assertThat(hearingEntity.getNotes()).isEqualTo("updated notes");
        assertThat(hearingEntity.getIssueNotice()).isEqualTo(VerticalYesNo.YES);
        assertThat(hearingEntity.getIsWithoutNotice()).isEqualTo(VerticalYesNo.YES);
        assertThat(hearingEntity.getAdditionalInformation()).isEqualTo("updated additional information");
        assertThat(hearingEntity.getNoticeParties()).containsExactly(partyId);
    }
}
