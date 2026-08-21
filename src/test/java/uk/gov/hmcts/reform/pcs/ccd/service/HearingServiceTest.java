package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.exception.HearingNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private PartyService partyService;

    private HearingService hearingService;

    @BeforeEach
    void setUp() {
        hearingService = new HearingService(
            pcsCaseService,
            pcsCaseRepository,
            hearingRepository,
            partyService,
            FIXED_UK_CLOCK
        );
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
            .durationHours(1f)
            .durationMinutes(30f)
            .notes("notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .additionalInformation("additional information")
            .build();

        UUID partyId = UUID.randomUUID();
        List<DynamicStringListElement> listItems = List.of(
            DynamicStringListElement.builder()
                .code(partyId.toString())
                .build()
        );
        DynamicMultiSelectStringList partyList = DynamicMultiSelectStringList.builder()
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
            .durationHours(1f)
            .durationMinutes(30f)
            .notes("notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.NO)
            .additionalInformation("additional information")
            .build();

        UUID partyId = UUID.randomUUID();
        List<DynamicStringListElement> listItems = List.of(
            DynamicStringListElement.builder()
                .code(partyId.toString())
                .build()
        );
        DynamicMultiSelectStringList partyList = DynamicMultiSelectStringList.builder()
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
            .durationHours(2f)
            .durationMinutes(15f)
            .issueNotice(VerticalYesNo.NO)
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicMultiSelectStringList partyList = DynamicMultiSelectStringList.builder()
            .value(List.of(DynamicStringListElement.builder().code(partyId.toString()).build()))
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
            .durationHours(3f)
            .durationMinutes(45f)
            .notes("updated notes")
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .additionalInformation("updated additional information")
            .build();

        UUID partyId = UUID.randomUUID();
        DynamicMultiSelectStringList partyList = DynamicMultiSelectStringList.builder()
            .value(List.of(DynamicStringListElement.builder().code(partyId.toString()).build()))
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
            .durationHours(1f)
            .durationMinutes(0f)
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
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                                         .value(List.of(DynamicStringListElement.builder()
                                                            .code(UUID.randomUUID().toString())
                                                            .build()))
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
        DynamicStringListElement selectedParty = DynamicStringListElement.builder()
            .code(UUID.randomUUID().toString())
            .label("Defendant - Defendant 1")
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .hearing(Hearing.builder()
                .type(HearingType.POSSESSION)
                .noticeWording(HearingNoticeWording.TPL)
                .date(LocalDateTime.of(2026, 8, 5, 10, 30))
                .durationDays(0)
                .durationHours(1f)
                .durationMinutes(30f)
                .notes("stale notes")
                .issueNotice(VerticalYesNo.YES)
                .isWithoutNotice(VerticalYesNo.YES)
                .additionalInformation("stale information")
                .build())
            .manageHearingDraft(Hearing.builder()
                .notes("draft notes")
                .build())
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                .value(List.of(selectedParty))
                .listItems(List.of(selectedParty))
                .build())
            .mhDraftPartyList(DynamicMultiSelectStringList.builder()
                .value(List.of(selectedParty))
                .listItems(List.of(selectedParty))
                .build())
            .build();

        // When
        hearingService.clearHearingForm(pcsCase);

        // Then
        assertThat(pcsCase.getHearing()).usingRecursiveComparison()
            .isEqualTo(Hearing.builder().build());
        assertThat(pcsCase.getManageHearingDraft()).isNull();
        assertThat(pcsCase.getPartyMultiSelectionList().getValue()).isEmpty();
        assertThat(pcsCase.getPartyMultiSelectionList().getListItems()).containsExactly(selectedParty);
        assertThat(pcsCase.getMhDraftPartyList()).isNull();
    }

    @Test
    void shouldBindNoticeRecipientsFromCcdDynamicMultiSelectPayloadWithStringCodes()
        throws JsonProcessingException {
        // Given
        UUID partyId = UUID.randomUUID();
        DynamicMultiSelectStringList selectedParties = new ObjectMapper().readValue("""
            {
                "value": [
                  {
                    "code": "%s",
                    "label": "Defendant - Defendant 1"
                  }
                ],
                "list_items": [
                  {
                    "code": "%s",
                    "label": "Defendant - Defendant 1"
                  }
                ]
            }
            """.formatted(partyId, partyId), DynamicMultiSelectStringList.class);

        // Then
        assertThat(selectedParties.getValue())
            .extracting(DynamicStringListElement::getCode)
            .containsExactly(partyId.toString());
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
        DynamicStringListElement selectedParty = DynamicStringListElement.builder()
            .code(selectedPartyId.toString())
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
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                .listItems(List.of(selectedParty))
                .build())
            .build();

        // When
        hearingService.prepopulateEditableHearing(caseReference, pcsCase);

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("1");
        assertThat(pcsCase.getHearing()).usingRecursiveComparison().isEqualTo(
            Hearing.builder()
                .hearingId(1)
                .type(HearingType.OTHER)
                .otherHearingType("case management")
                .noticeWording(HearingNoticeWording.RES)
                .date(LocalDateTime.of(2026, 8, 3, 14, 30))
                .durationDays(1)
                .durationHours(2f)
                .durationMinutes(45f)
                .notes("existing notes")
                .issueNotice(VerticalYesNo.YES)
                .isWithoutNotice(VerticalYesNo.YES)
                .additionalInformation("existing information")
                .build()
        );
        assertThat(pcsCase.getPartyMultiSelectionList().getValue())
            .extracting(DynamicStringListElement::getCode)
            .containsExactly(selectedPartyId.toString());
    }

    @Test
    void shouldPreserveHearingSummaryMarkdownWhenPrepopulatingEditableHearing() {
        // Given
        long caseReference = 12345L;
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 14, 30))
            .durationDays(0)
            .durationHours(1)
            .durationMinutes(30)
            .notes("existing notes")
            .issueNotice(VerticalYesNo.NO)
            .additionalInformation("existing information")
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("1")
            .hearing(Hearing.builder()
                .hearingId(1)
                .hearingSummaryMarkdown("existing hearing summary")
                .build())
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                .listItems(List.of(DynamicStringListElement.builder()
                    .code(UUID.randomUUID().toString())
                    .build()))
                .build())
            .build();

        // When
        hearingService.prepopulateEditableHearing(caseReference, pcsCase);

        // Then
        assertThat(pcsCase.getHearing().getNotes()).isEqualTo("existing notes");
        assertThat(pcsCase.getHearing().getHearingSummaryMarkdown()).isEqualTo("existing hearing summary");
    }

    @Test
    void shouldInitialiseEditableHearingAndPrepopulateWhenSelectedHearingChanges() {
        // Given
        long caseReference = 12345L;
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 14, 30))
            .durationDays(0)
            .durationHours(1)
            .durationMinutes(30)
            .notes("existing notes")
            .issueNotice(VerticalYesNo.NO)
            .additionalInformation("existing information")
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                .listItems(List.of(DynamicStringListElement.builder()
                    .code(UUID.randomUUID().toString())
                    .build()))
                .build())
            .build();

        // When
        hearingService.initialiseEditableHearing(caseReference, pcsCase, null);

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("1");
        assertThat(pcsCase.getHearing().getNotes()).isEqualTo("existing notes");
        assertThat(pcsCase.getHearing().getAdditionalInformation()).isEqualTo("existing information");
    }

    @Test
    void shouldInitialiseEditableHearingAndPrepopulateWhenSelectedHearingIsAlreadySetButNoDraftExists() {
        // Given
        long caseReference = 12345L;
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 14, 30))
            .durationDays(0)
            .durationHours(1)
            .durationMinutes(30)
            .notes("existing notes")
            .issueNotice(VerticalYesNo.NO)
            .additionalInformation("existing information")
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("1")
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                .listItems(List.of(DynamicStringListElement.builder()
                    .code(UUID.randomUUID().toString())
                    .build()))
                .build())
            .build();

        // When
        hearingService.initialiseEditableHearing(caseReference, pcsCase, "1");

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("1");
        assertThat(pcsCase.getHearing().getNotes()).isEqualTo("existing notes");
        assertThat(pcsCase.getHearing().getAdditionalInformation()).isEqualTo("existing information");
    }

    @Test
    void shouldInitialiseEditableHearingAndPrepopulateWhenRetainedDraftIsEmpty() {
        // Given
        long caseReference = 12345L;
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 14, 30))
            .durationDays(0)
            .durationHours(1)
            .durationMinutes(30)
            .notes("existing notes")
            .issueNotice(VerticalYesNo.NO)
            .additionalInformation("existing information")
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("1")
            .manageHearingDraft(Hearing.builder().build())
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                .listItems(List.of(DynamicStringListElement.builder()
                    .code(UUID.randomUUID().toString())
                    .build()))
                .build())
            .build();

        // When
        hearingService.initialiseEditableHearing(caseReference, pcsCase, "1");

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("1");
        assertThat(pcsCase.getHearing().getNotes()).isEqualTo("existing notes");
        assertThat(pcsCase.getHearing().getAdditionalInformation()).isEqualTo("existing information");
    }

    @Test
    void shouldInitialiseEditableHearingWithoutReplacingDraftChangesWhenSelectedHearingIsUnchanged() {
        // Given
        long caseReference = 12345L;
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 14, 30))
            .durationDays(0)
            .durationHours(1)
            .durationMinutes(30)
            .notes("existing notes")
            .issueNotice(VerticalYesNo.NO)
            .additionalInformation("existing information")
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("1")
            .manageHearingDraft(Hearing.builder()
                .notes("draft notes")
                .additionalInformation("draft information")
                .build())
            .build();

        // When
        hearingService.initialiseEditableHearing(caseReference, pcsCase, "1");

        // Then
        assertThat(pcsCase.getSelectedHearingId()).isEqualTo("1");
        assertThat(pcsCase.getHearing().getNotes()).isEqualTo("draft notes");
        assertThat(pcsCase.getHearing().getAdditionalInformation()).isEqualTo("draft information");
    }

    @Test
    void shouldStoreDraftHearingForm() {
        // Given
        DynamicMultiSelectStringList selectedParties = DynamicMultiSelectStringList.builder()
            .value(List.of(DynamicStringListElement.builder()
                .code(UUID.randomUUID().toString())
                .build()))
            .build();
        PCSCase pcsCase = PCSCase.builder()
            .hearing(Hearing.builder()
                .notes("draft notes")
                .additionalInformation("draft information")
                .build())
            .partyMultiSelectionList(selectedParties)
            .build();

        // When
        hearingService.storeDraftHearingForm(pcsCase);

        // Then
        assertThat(pcsCase.getManageHearingDraft()).usingRecursiveComparison()
            .isEqualTo(pcsCase.getHearing());
        assertThat(pcsCase.getMhDraftPartyList()).isSameAs(selectedParties);
    }

    @Test
    void shouldBuildPartyListBeforePreselectingNoticeRecipientsWhenPartyListIsMissing() {
        // Given
        long caseReference = 12345L;
        UUID selectedPartyId = UUID.randomUUID();
        PartyEntity selectedParty = PartyEntity.builder()
            .id(selectedPartyId)
            .firstName("Claimant")
            .lastName("One")
            .build();
        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(List.of(ClaimPartyEntity.builder()
                .role(PartyRole.CLAIMANT)
                .party(selectedParty)
                .build()))
            .build();
        HearingEntity selectedHearing = HearingEntity.builder()
            .id(1)
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.RES)
            .hearingDate(LocalDateTime.of(2026, 8, 3, 14, 30))
            .durationDays(1)
            .durationHours(2)
            .durationMinutes(45)
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .noticeParties(List.of(selectedPartyId))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(List.of(mainClaim))
            .hearings(List.of(selectedHearing))
            .build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(partyService.getPartyName(selectedParty)).thenReturn("Claimant One");
        when(partyService.getPartyLabel(mainClaim, selectedPartyId)).thenReturn("Claimant 1");

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("1")
            .hearing(Hearing.builder().build())
            .build();

        // When
        hearingService.prepopulateEditableHearing(caseReference, pcsCase);

        // Then
        assertThat(pcsCase.getPartyMultiSelectionList()).isNotNull();
        assertThat(pcsCase.getPartyMultiSelectionList().getListItems())
            .extracting(DynamicStringListElement::getCode)
            .containsExactly(selectedPartyId.toString());
        assertThat(pcsCase.getPartyMultiSelectionList().getValue())
            .extracting(DynamicStringListElement::getCode)
            .containsExactly(selectedPartyId.toString());
    }

    @Test
    void shouldUpdateHearingNoticePartiesFromSelectedDynamicMultiSelectValues() {
        // Given
        UUID partyId = UUID.randomUUID();
        Hearing hearing = Hearing.builder()
            .type(HearingType.APPLICATION)
            .noticeWording(HearingNoticeWording.TPL)
            .date(LocalDateTime.of(2026, 4, 5, 11, 30, 0))
            .durationDays(0)
            .durationHours(1f)
            .durationMinutes(0f)
            .issueNotice(VerticalYesNo.YES)
            .isWithoutNotice(VerticalYesNo.YES)
            .build();

        HearingEntity selectedHearing = HearingEntity.builder()
            .id(10)
            .noticeParties(List.of())
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .hearings(List.of(selectedHearing))
            .build();
        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        PCSCase pcsCase = PCSCase.builder()
            .selectedHearingId("10")
            .partyMultiSelectionList(DynamicMultiSelectStringList.builder()
                .value(List.of(DynamicStringListElement.builder()
                    .code(partyId.toString())
                    .build()))
                .build())
            .hearing(hearing)
            .build();

        // When
        hearingService.updateHearing(caseReference, pcsCase);

        ArgumentCaptor<PcsCaseEntity> pcsCaseEntityCaptor = ArgumentCaptor.forClass(PcsCaseEntity.class);
        verify(pcsCaseRepository).save(pcsCaseEntityCaptor.capture());

        // Then
        assertThat(pcsCaseEntityCaptor.getValue().getHearings().getFirst().getNoticeParties())
            .containsExactly(partyId);
    }

    @Test
    void shouldCancelHearing() {
        // Given
        int hearingId = 5678;
        String expectedCancellationReason = "some cancellation reason";

        Hearing hearing = Hearing.builder()
            .hearingId(hearingId)
            .cancellationReason(expectedCancellationReason)
            .build();
        HearingEntity hearingEntity = HearingEntity.builder().build();

        when(hearingRepository.findById(hearingId)).thenReturn(Optional.of(hearingEntity));

        // When
        hearingService.cancelHearing(hearing);

        // Then
        assertThat(hearingEntity.getCancelled()).isTrue();
        assertThat(hearingEntity.getCancellationReason()).isEqualTo(expectedCancellationReason);
    }

    @Test
    void shouldThrowHearingNotFoundExceptionWhenCancellingMissingHearing() {
        // Given
        int hearingId = 5678;

        Hearing hearing = Hearing.builder()
            .hearingId(hearingId)
            .build();

        when(hearingRepository.findById(hearingId)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> hearingService.cancelHearing(hearing));

        // Then
        assertThat(throwable)
            .isInstanceOf(HearingNotFoundException.class)
            .hasMessage("REDACTED [HEARING_NOT_FOUND]");
    }
}
