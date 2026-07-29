package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
public class HearingServiceTest {

    private static final Clock FIXED_UK_CLOCK = Clock.fixed(
        Instant.parse("2026-08-01T09:00:00Z"),
        UK_ZONE_ID
    );

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    private HearingService hearingService;

    @BeforeEach
    void setUp() {
        hearingService = new HearingService(pcsCaseService, pcsCaseRepository, FIXED_UK_CLOCK);
    }

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
    void shouldAddNonOtherHearingWithoutOtherTypeOrNoticeParties() {
        // Given
        Hearing hearing = Hearing.builder()
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .date(LocalDateTime.of(2026, 2, 1, 9,  0, 0))
            .durationDays(0)
            .durationHours(2)
            .durationMinutes(15)
            .issueNotice(VerticalYesNo.NO)
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicMultiSelectList partyList = DynamicMultiSelectList.builder()
            .value(List.of(DynamicListElement.builder().code(partyId).build()))
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

        // Then
        HearingEntity hearingEntity = pcsCaseEntityCaptor.getValue().getHearings().getFirst();
        assertThat(hearingEntity.getType()).isEqualTo(HearingType.APPLICATION);
        assertThat(hearingEntity.getOtherHearingType()).isNull();
        assertThat(hearingEntity.getIssueNotice()).isEqualTo(VerticalYesNo.NO);
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

    @Test
    void shouldClearStaleOtherTypeAndNoticePartiesWhenUpdatingSelectedHearing() {
        // Given
        Hearing hearing = Hearing.builder()
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .date(LocalDateTime.of(2026, 4, 5, 11, 30, 0))
            .durationDays(0)
            .durationHours(1)
            .durationMinutes(0)
            .issueNotice(VerticalYesNo.NO)
            .build();

        UUID oldPartyId = UUID.randomUUID();
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(10)
            .type(HearingType.OTHER)
            .otherHearingType("old custom type")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .noticeParties(List.of(oldPartyId))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("10")
            .hearing(hearing)
            .partyMultiSelectionList(DynamicMultiSelectList.builder()
                                         .value(List.of(DynamicListElement.builder().code(UUID.randomUUID()).build()))
                                         .build())
            .build();

        // When
        hearingService.updateHearing(caseReference, pcsCase);

        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        // Then
        HearingEntity hearingEntity = pcsCaseEntityCaptor.getValue().getHearings().getFirst();
        assertThat(hearingEntity.getId()).isEqualTo(10);
        assertThat(hearingEntity.getType()).isEqualTo(HearingType.APPLICATION);
        assertThat(hearingEntity.getOtherHearingType()).isNull();
        assertThat(hearingEntity.getIssueNotice()).isEqualTo(VerticalYesNo.NO);
        assertThat(hearingEntity.getNoticeParties()).isEmpty();
    }

    @Test
    void shouldClearHearingFormAndSelectedNoticeRecipients() {
        // Given
        DynamicListElement selectedParty = DynamicListElement.builder()
            .code(UUID.randomUUID())
            .label("Defendant - Defendant 1")
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .hearing(Hearing.builder()
                .type(HearingType.POSSESSION)
                .noticeWording(HearingNoticeWording.TPL)
                .date(LocalDateTime.of(2026, 8, 5, 10, 30))
                .durationDays(0)
                .durationHours(1)
                .durationMinutes(30)
                .notes("stale notes")
                .issueNotice(VerticalYesNo.YES)
                .isWithoutNotice(VerticalYesNo.YES)
                .additionalInformation("stale information")
                .build())
            .partyMultiSelectionList(DynamicMultiSelectList.builder()
                .value(List.of(selectedParty))
                .listItems(List.of(selectedParty))
                .build())
            .build();

        // When
        hearingService.clearHearingForm(pcsCase);

        // Then
        assertThat(pcsCase.getHearing()).usingRecursiveComparison()
            .isEqualTo(Hearing.builder().build());
        assertThat(pcsCase.getPartyMultiSelectionList().getValue()).isNull();
        assertThat(pcsCase.getPartyMultiSelectionList().getListItems()).containsExactly(selectedParty);
    }

    @Test
    void shouldSelectNextUpcomingHearingByScheduledDate() {
        // Given
        HearingEntity laterUpcomingHearing = HearingEntity.builder()
            .id(1)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 9, 0))
            .build();
        HearingEntity pastHearing = HearingEntity.builder()
            .id(2)
            .hearingDate(LocalDateTime.of(2026, 7, 31, 9, 0))
            .build();
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(3)
            .hearingDate(LocalDateTime.of(2026, 8, 2, 14, 30))
            .build();
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(laterUpcomingHearing, pastHearing, selectedHearing))
            .build();

        // When
        hearingService.setSelectedEditableHearingId(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("3");
    }

    @Test
    void shouldSelectLatestPastHearingWhenThereAreNoUpcomingHearings() {
        // Given
        HearingEntity olderPastHearing = HearingEntity.builder()
            .id(1)
            .hearingDate(LocalDateTime.of(2020, 1, 1, 10, 15))
            .build();
        HearingEntity latestPastHearing = HearingEntity.builder()
            .id(4)
            .hearingDate(LocalDateTime.of(2026, 7, 31, 9, 0))
            .build();
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(olderPastHearing, latestPastHearing))
            .build();

        // When
        hearingService.setSelectedEditableHearingId(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("4");
    }

    @Test
    void shouldPrepopulateEditableHearingAndNoticeRecipients() {
        // Given
        long caseReference = 12345L;
        UUID selectedPartyId = UUID.randomUUID();
        DynamicListElement selectedParty = DynamicListElement.builder()
            .code(selectedPartyId)
            .label("Defendant - Defendant 1")
            .build();
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.OTHER)
            .otherHearingType("case management")
            .noticeWording(HearingNoticeWording.RES)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 14, 30))
            .durationDays(1)
            .durationHours(2)
            .durationMinutes(45)
            .notes("existing notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .noticeParties(List.of(selectedPartyId))
            .additionalInformation("existing information")
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("1")
            .hearing(Hearing.builder().build())
            .partyMultiSelectionList(DynamicMultiSelectList.builder()
                .listItems(List.of(selectedParty))
                .build())
            .build();

        // When
        hearingService.prepopulateEditableHearing(caseReference, pcsCase);

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("1");
        assertThat(pcsCase.getHearing()).usingRecursiveComparison().isEqualTo(
            Hearing.builder()
                .type(HearingType.OTHER)
                .otherHearingType("case management")
                .noticeWording(HearingNoticeWording.RES)
                .date(LocalDateTime.of(2026, 8, 3, 14, 30))
                .durationDays(1)
                .durationHours(2)
                .durationMinutes(45)
                .notes("existing notes")
                .issueNotice(VerticalYesNo.YES)
                .isWithoutNotice(VerticalYesNo.YES)
                .additionalInformation("existing information")
                .build()
        );
        assertThat(pcsCase.getPartyMultiSelectionList().getValue())
            .extracting(DynamicListElement::getCode)
            .containsExactly(selectedPartyId);
    }
}
