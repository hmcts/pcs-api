package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftCaseDataServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String OWNER_ORGANISATION_ID = "QKLHPMU";
    private static final String OTHER_ORGANISATION_ID = "IHOVCKH";
    private static final EventId PARTY_OWNED_EVENT = EventId.resumePossessionClaim;

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;
    @Mock
    private OrganisationService organisationService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private DraftCaseJsonMerger draftCaseJsonMerger;
    @Mock
    private SecurityContextService securityContextService;
    @Captor
    private ArgumentCaptor<DraftCaseDataEntity> unsubmittedCaseDataEntityCaptor;

    /** A user-owned journey: these tests cover lookup mechanics rather than who owns the draft. */
    private final EventId eventId = EventId.respondPossessionClaim;

    private DraftCaseDataService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftCaseDataService(
            draftCaseDataRepository,
            organisationService,
            objectMapper,
            draftCaseJsonMerger,
            securityContextService
        );
    }

    @Test
    void shouldGetUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        PCSCase expectedUnsubmittedCaseData = mock(PCSCase.class);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenReturn(expectedUnsubmittedCaseData);
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(unsubmittedCaseData).contains(expectedUnsubmittedCaseData);
        verify(expectedUnsubmittedCaseData).setHasUnsubmittedCaseData(YesOrNo.YES);
    }

    @Test
    void shouldReturnEmptyWhenNoUnsubmittedCaseData() {
        // Given
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.empty());
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(unsubmittedCaseData).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnWhetherUnsubmittedCaseDataExists(boolean repositoryDataExists) {
        // Given
        when(draftCaseDataRepository
                 .existsByCaseReferenceAndEventIdAndIdamUserId(
                     CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(repositoryDataExists);
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        boolean hasUnsubmittedCaseData = underTest.hasUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(hasUnsubmittedCaseData).isEqualTo(repositoryDataExists);
    }

    @Test
    void shouldSaveNewUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String caseDataJson = "case data json";
        PCSCase caseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(caseData)).thenReturn(caseDataJson);
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.empty());
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getCaseData()).isEqualTo(caseDataJson);
        assertThat(savedEntity.getIdamUserId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldPatchUnsubmittedCaseDataWithJson() {
        // Given
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.empty());
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        String caseDataJson = "case data json";
        // When
        underTest.patchUnsubmittedCaseData(CASE_REFERENCE, eventId, caseDataJson);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getCaseData()).isEqualTo(caseDataJson);
    }

    @Test
    void shouldUpdateExistingUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String existingCaseDataJson = "existing case data json";
        String newCaseDataJson = "new case data json";
        String mergedCaseDataJson = "merged case data JSON";

        PCSCase newCaseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(newCaseData)).thenReturn(newCaseDataJson);

        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        when(draftCaseDataEntity.getCaseData()).thenReturn(existingCaseDataJson);

        when(draftCaseJsonMerger.mergeJson(existingCaseDataJson, newCaseDataJson)).thenReturn(mergedCaseDataJson);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, newCaseData, eventId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity).isSameAs(draftCaseDataEntity);
        verify(draftCaseDataEntity).setCaseData(mergedCaseDataJson);
    }

    @Test
    void shouldThrowWhenNoDraftExistsForSave() throws JsonProcessingException {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(objectMapper.writeValueAsString(caseData)).thenReturn("case data json");
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.saveUnsubmittedEventData(CASE_REFERENCE, caseData, eventId))
            .isInstanceOf(UnsubmittedDataException.class)
            .hasMessageContaining("No draft found");

        verify(draftCaseDataRepository, never()).save(any());
    }

    @Test
    void shouldReplaceExistingUnsubmittedEventData() throws JsonProcessingException {
        // Given
        String newCaseDataJson = "new case data json";
        PCSCase newCaseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(newCaseData)).thenReturn(newCaseDataJson);

        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        underTest.saveUnsubmittedEventData(CASE_REFERENCE, newCaseData, eventId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity).isSameAs(draftCaseDataEntity);
        verify(draftCaseDataEntity).setCaseData(newCaseDataJson);
    }

    @Test
    void shouldDeleteUnsubmittedDataByCaseReference() {
        // given
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        underTest.deleteUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        verify(draftCaseDataRepository)
            .deleteByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, eventId, USER_ID);
    }

    @Test
    void shouldDeleteUnsubmittedDataByCaseReferenceAndPartyId() {
        // given
        UUID partyId = UUID.randomUUID();
        String orgId = UUID.randomUUID().toString();

        // When
        underTest.deleteUnsubmittedCaseData(CASE_REFERENCE, eventId, partyId, orgId);

        // Then
        verify(draftCaseDataRepository)
            .deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                        orgId, partyId);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenReading() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);

        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenThrow(jsonProcessingException);
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // Then
        assertThatThrownBy(() -> underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId))
            .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to read saved answers")
            .hasCause(jsonProcessingException);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenSaving() throws JsonProcessingException {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(caseData)).thenThrow(jsonProcessingException);
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // Then
        assertThatThrownBy(() -> underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId))
           .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to save answers")
            .hasCause(jsonProcessingException);

    }

    @Test
    void shouldGetUnsubmittedCaseDataForPartyId() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        PCSCase expectedUnsubmittedCaseData = mock(PCSCase.class);
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();

        when(draftCaseDataRepository
                 .findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                           organisationId, partyId))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenReturn(expectedUnsubmittedCaseData);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId, partyId,
                                                                                 organisationId);

        // Then
        assertThat(unsubmittedCaseData).contains(expectedUnsubmittedCaseData);
        verify(expectedUnsubmittedCaseData).setHasUnsubmittedCaseData(YesOrNo.YES);
    }

    @Test
    void shouldReturnEmptyWhenNoUnsubmittedCaseDataForPartyId() {
        // Given
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();
        when(draftCaseDataRepository
                 .findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                           organisationId, partyId))
            .thenReturn(Optional.empty());

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId, partyId,
                                                                                 organisationId);

        // Then
        assertThat(unsubmittedCaseData).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnWhetherUnsubmittedCaseDataExistsForPartyId(boolean repositoryDataExists) {
        // Given
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();
        when(draftCaseDataRepository
                 .existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                             organisationId, partyId))
            .thenReturn(repositoryDataExists);

        // When
        boolean hasUnsubmittedCaseData = underTest.hasUnsubmittedCaseData(CASE_REFERENCE, eventId, partyId,
                                                                          organisationId);

        // Then
        assertThat(hasUnsubmittedCaseData).isEqualTo(repositoryDataExists);
    }

    @Test
    void shouldSaveNewUnsubmittedCaseData_WithPartyId() throws JsonProcessingException {
        // Given
        String caseDataJson = "case data json";
        PCSCase caseData = mock(PCSCase.class);
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();
        when(objectMapper.writeValueAsString(caseData)).thenReturn(caseDataJson);
        when(draftCaseDataRepository
                 .findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                           organisationId, partyId))
            .thenReturn(Optional.empty());
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId, partyId, organisationId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getCaseData()).isEqualTo(caseDataJson);
        assertThat(savedEntity.getOrganisationId()).isEqualTo(organisationId);
        assertThat(savedEntity.getPartyId()).isEqualTo(partyId);
    }

    @Test
    void shouldUpdateExistingUnsubmittedCaseDataWithPartId() throws JsonProcessingException {
        // Given
        String existingCaseDataJson = "existing case data json";
        String newCaseDataJson = "new case data json";
        String mergedCaseDataJson = "merged case data JSON";
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();
        PCSCase newCaseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(newCaseData)).thenReturn(newCaseDataJson);

        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        when(draftCaseDataEntity.getCaseData()).thenReturn(existingCaseDataJson);

        when(draftCaseJsonMerger.mergeJson(existingCaseDataJson, newCaseDataJson)).thenReturn(mergedCaseDataJson);

        when(draftCaseDataRepository
                 .findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                           organisationId, partyId))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, newCaseData, eventId, partyId, organisationId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity).isSameAs(draftCaseDataEntity);
        verify(draftCaseDataEntity).setCaseData(mergedCaseDataJson);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenSaving_WithPartyId() throws JsonProcessingException {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(caseData)).thenThrow(jsonProcessingException);
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();

        // Then
        assertThatThrownBy(() -> underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId, partyId,
                                                                     organisationId))
            .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to save answers")
            .hasCause(jsonProcessingException);
    }

    @Test
    void shouldPatchUnsubmittedCaseData_WithPartyId() {
        // Given
        String existingJson = "existing json";
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);

        when(draftCaseDataEntity.getCaseData()).thenReturn(existingJson);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(
            CASE_REFERENCE, eventId, organisationId, partyId))
            .thenReturn(Optional.of(draftCaseDataEntity));
        String patchJson = "patch json";
        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, patchJson, eventId, partyId, organisationId);

        // Then
        verify(draftCaseDataEntity).getCaseData();
        verify(draftCaseDataEntity).setCaseData(any()); // safer than strict match here
        verify(draftCaseDataRepository).save(draftCaseDataEntity);
    }

    @Test
    void shouldThrowWhenNoDraftExistsForSave_WithPartyId() throws JsonProcessingException {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();
        when(objectMapper.writeValueAsString(caseData)).thenReturn("case data json");

        when(draftCaseDataRepository
                 .findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                           organisationId, partyId))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.saveUnsubmittedEventData(CASE_REFERENCE, caseData, eventId, partyId,
                                                                    organisationId))
            .isInstanceOf(UnsubmittedDataException.class)
            .hasMessageContaining("No draft found");

        verify(draftCaseDataRepository, never()).save(any());
    }

    @Test
    void shouldReplaceExistingUnsubmittedEventData_WithPartyId() throws JsonProcessingException {

        // Given
        String newCaseDataJson = "new case data json";
        UUID partyId = UUID.randomUUID();
        String organisationId = UUID.randomUUID().toString();

        PCSCase newCaseData = mock(PCSCase.class);

        when(objectMapper.writeValueAsString(newCaseData)).thenReturn(newCaseDataJson);

        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);

        when(draftCaseDataRepository
                 .findByCaseReferenceAndEventIdAndOrganisationIdAndPartyId(CASE_REFERENCE, eventId,
                                                                           organisationId, partyId))
            .thenReturn(Optional.of(draftCaseDataEntity));

        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        underTest.saveUnsubmittedEventData(CASE_REFERENCE, newCaseData, eventId, partyId, organisationId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());

        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity).isSameAs(draftCaseDataEntity);

        verify(draftCaseDataEntity).setCaseData(newCaseDataJson);
    }

    @Test
    void shouldFindDraftByOwningOrganisationForClaimJourney() throws JsonProcessingException {
        // Given
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        String draftJson = "colleague draft json";
        DraftCaseDataEntity colleagueDraft = mock(DraftCaseDataEntity.class);
        PCSCase expected = mock(PCSCase.class);

        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(OWNER_ORGANISATION_ID);
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
            CASE_REFERENCE, PARTY_OWNED_EVENT, OWNER_ORGANISATION_ID)).thenReturn(Optional.of(colleagueDraft));
        when(colleagueDraft.getCaseData()).thenReturn(draftJson);
        when(objectMapper.readValue(draftJson, PCSCase.class)).thenReturn(expected);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT);

        // Then
        assertThat(unsubmittedCaseData).contains(expected);
    }

    @Test
    void shouldReportUnsubmittedDataExistsForAnyMemberOfTheOwningFirm() {
        // Given
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(OWNER_ORGANISATION_ID);
        when(draftCaseDataRepository.existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
            CASE_REFERENCE, PARTY_OWNED_EVENT, OWNER_ORGANISATION_ID)).thenReturn(true);

        // When / Then
        assertThat(underTest.hasUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT)).isTrue();
    }

    @Test
    void shouldStampOwningOrganisationOnNewClaimDraft() {
        // Given
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(OWNER_ORGANISATION_ID);
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
            CASE_REFERENCE, PARTY_OWNED_EVENT, OWNER_ORGANISATION_ID)).thenReturn(Optional.empty());
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        underTest.patchUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT, "case data json");

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getOrganisationId()).isEqualTo(OWNER_ORGANISATION_ID);
        assertThat(savedEntity.getIdamUserId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldDeleteClaimDraftByOwningOrganisation() {
        // Given
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(OWNER_ORGANISATION_ID);

        // When
        underTest.deleteUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT);

        // Then
        verify(draftCaseDataRepository)
            .deleteByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
                CASE_REFERENCE, PARTY_OWNED_EVENT, OWNER_ORGANISATION_ID);
    }

    @Test
    void shouldKeepDraftPrivateToTheUserWhenTheyHaveNoOrganisation() {
        // Given a citizen, who belongs to no organisation
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(null);
        when(draftCaseDataRepository.existsByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, PARTY_OWNED_EVENT, USER_ID)).thenReturn(true);

        // When / Then
        assertThat(underTest.hasUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT)).isTrue();
        verify(draftCaseDataRepository, never())
            .existsByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(anyLong(), any(), any());
    }

    /**
     * The legal representative journey writes drafts for the same case, event and organisation but
     * with a party. The claim journey must not pick those up: it would surface a defendant's answers
     * on the claimant side, and a firm representing two defendants would make the lookup non-unique.
     */
    @Test
    void shouldNotReadALegalRepresentativePartyDraftFromTheClaimJourney() {
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(OWNER_ORGANISATION_ID);
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
            CASE_REFERENCE, PARTY_OWNED_EVENT, OWNER_ORGANISATION_ID)).thenReturn(Optional.empty());
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
            CASE_REFERENCE, PARTY_OWNED_EVENT, USER_ID)).thenReturn(Optional.empty());

        assertThat(underTest.getUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT)).isEmpty();

        // the unconstrained user-keyed lookup must not be used either: it would match a party draft
        verify(draftCaseDataRepository, never())
            .findByCaseReferenceAndEventIdAndIdamUserId(anyLong(), any(), any());
    }

    @Test
    void shouldFailRatherThanSilentlyKeepTheDraftToOneUserWhenTheOrganisationCannotBeResolved() {
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        when(organisationService.requireOrganisationIdForCurrentUser())
            .thenThrow(new OrganisationDetailsException("rd-professional unavailable", new RuntimeException()));

        assertThatThrownBy(() -> underTest.getUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT))
            .isInstanceOf(UnsubmittedDataException.class);

        verifyNoInteractions(draftCaseDataRepository);
    }

    @Test
    void shouldKeepTheDraftUserKeyedWhenAProfessionalGenuinelyHasNoOrganisation() {
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(null);
        when(draftCaseDataRepository.existsByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, PARTY_OWNED_EVENT, USER_ID)).thenReturn(true);

        assertThat(underTest.hasUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT)).isTrue();
    }

    @Test
    void shouldNotAskRdProfessionalForACitizen() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(
            UserInfo.builder().uid(USER_ID.toString()).roles(List.of(UserRole.CITIZEN.getRole())).build());
        when(draftCaseDataRepository.existsByCaseReferenceAndEventIdAndIdamUserId(
            CASE_REFERENCE, PARTY_OWNED_EVENT, USER_ID)).thenReturn(true);

        assertThat(underTest.hasUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT)).isTrue();

        verify(organisationService, never()).getOrganisationIdForCurrentUser();
    }

    @Test
    void shouldAdoptADraftSavedBeforeOrganisationKeying() throws JsonProcessingException {
        // Given a draft written when drafts were keyed on the user alone
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().uid(USER_ID.toString()).build());
        String draftJson = "pre-existing draft json";
        DraftCaseDataEntity legacyDraft = new DraftCaseDataEntity();
        legacyDraft.setCaseData(draftJson);
        PCSCase expected = mock(PCSCase.class);

        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn(OWNER_ORGANISATION_ID);
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndOrganisationIdAndPartyIdIsNull(
            CASE_REFERENCE, PARTY_OWNED_EVENT, OWNER_ORGANISATION_ID)).thenReturn(Optional.empty());
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserIdAndPartyIdIsNull(
            CASE_REFERENCE, PARTY_OWNED_EVENT, USER_ID)).thenReturn(Optional.of(legacyDraft));
        when(draftCaseDataRepository.save(legacyDraft)).thenReturn(legacyDraft);
        when(objectMapper.readValue(draftJson, PCSCase.class)).thenReturn(expected);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, PARTY_OWNED_EVENT);

        // Then the answers survive, now owned by the organisation
        assertThat(unsubmittedCaseData).contains(expected);
        assertThat(legacyDraft.getOrganisationId()).isEqualTo(OWNER_ORGANISATION_ID);
    }

}
