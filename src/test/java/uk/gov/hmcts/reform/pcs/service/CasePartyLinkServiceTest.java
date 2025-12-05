package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.model.ValidateAccessCodeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasePartyLinkServiceTest {

    private static final long CASE_REFERENCE = 12345L;
    private static final String ACCESS_CODE = "ABC123XYZ789";
    private static final String USER_ID = "user-123";
    private static final String OTHER_USER_ID = "user-456";

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PartyAccessCodeRepository pacRepository;

    @Captor
    private ArgumentCaptor<PcsCaseEntity> caseEntityCaptor;

    private CasePartyLinkService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CasePartyLinkService(pcsCaseService, pacRepository);
    }

    // Positive Tests

    @Test
    void shouldSuccessfullyLinkPartyWhenValidAccessCodeAndUnlinkedDefendant() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, partyId, null);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        ValidateAccessCodeResponse response = underTest.validateAndLinkParty(
                CASE_REFERENCE, ACCESS_CODE, userInfo);

        // Then
        assertThat(response.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(response.getStatus()).isEqualTo("linked");
        assertThat(caseEntity.getDefendants().get(0).getLinkedUserId()).isEqualTo(USER_ID);
        verify(pcsCaseService).save(caseEntityCaptor.capture());
        assertThat(caseEntityCaptor.getValue()).isSameAs(caseEntity);
    }

    @Test
    void shouldSaveCaseEntityWithUpdatedDefendantsAfterLinking() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, partyId, null);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo);

        // Then
        verify(pcsCaseService).save(caseEntityCaptor.capture());
        PcsCaseEntity savedEntity = caseEntityCaptor.getValue();
        assertThat(savedEntity).isSameAs(caseEntity);
        assertThat(savedEntity.getDefendants()).isNotNull();
        assertThat(savedEntity.getDefendants().get(0).getLinkedUserId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldSetLinkedUserIdOnMatchingDefendant() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, partyId, null);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo);

        // Then
        List<Defendant> defendants = caseEntity.getDefendants();
        assertThat(defendants).isNotEmpty();
        Defendant matchedDefendant = defendants.stream()
                .filter(d -> partyId.equals(d.getPartyId()))
                .findFirst()
                .orElse(null);
        assertThat(matchedDefendant).isNotNull();
        assertThat(matchedDefendant.getLinkedUserId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldReturnCorrectResponseWithCaseReferenceAndLinkedStatus() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, partyId, null);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        ValidateAccessCodeResponse response = underTest.validateAndLinkParty(
                CASE_REFERENCE, ACCESS_CODE, userInfo);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(response.getStatus()).isEqualTo("linked");
    }

    // Exception/Negative Tests

    @Test
    void shouldThrowIllegalStateExceptionWhenUserAlreadyLinked() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, partyId, OTHER_USER_ID);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        Throwable throwable = catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User already linked");
        verify(pcsCaseService, never()).save(caseEntity);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenSameUserCallsAgainWithAlreadyLinkedDefendant() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, partyId, USER_ID);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        Throwable throwable = catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User already linked");
        verify(pcsCaseService, never()).save(caseEntity);
    }

    @Test
    void shouldThrowCaseNotFoundExceptionWhenCaseReferenceNotFound() {
        // Given
        when(pcsCaseService.loadCase(CASE_REFERENCE))
                .thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        UserInfo userInfo = mock(UserInfo.class);

        // When
        Throwable throwable = catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        assertThat(throwable)
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage("No case found with reference " + CASE_REFERENCE);
        verify(pacRepository, never()).findByPcsCase_IdAndCode(null, ACCESS_CODE);
    }

    @Test
    void shouldThrowCaseNotFoundExceptionWhenAccessCodeNotFound() {
        // Given
        UUID caseId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, UUID.randomUUID(), null);
        UserInfo userInfo = mock(UserInfo.class);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        assertThat(throwable)
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage("No case found with reference " + CASE_REFERENCE);
        verify(pcsCaseService, never()).save(caseEntity);
    }

    @Test
    void shouldThrowCaseNotFoundExceptionWhenDefendantNotFoundByPartyId() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID differentPartyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, differentPartyId, null);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        Throwable throwable = catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        assertThat(throwable)
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage("No case found with reference " + CASE_REFERENCE);
        verify(pcsCaseService, never()).save(caseEntity);
    }

    @Test
    void shouldHandleNullDefendantsList() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(caseId);
        caseEntity.setDefendants(null);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        Throwable throwable = catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        assertThat(throwable)
                .isInstanceOf(NullPointerException.class);
        verify(pcsCaseService, never()).save(caseEntity);
    }

    @Test
    void shouldHandleEmptyDefendantsList() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(caseId);
        caseEntity.setDefendants(new ArrayList<>());
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        Throwable throwable = catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        assertThat(throwable)
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage("No case found with reference " + CASE_REFERENCE);
        verify(pcsCaseService, never()).save(caseEntity);
    }

    @Test
    void shouldNotSaveWhenUserAlreadyLinked() {
        // Given
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = createCaseEntity(caseId, partyId, OTHER_USER_ID);
        PartyAccessCodeEntity pac = createPartyAccessCodeEntity(partyId);
        UserInfo userInfo = createUserInfo(USER_ID);

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(pacRepository.findByPcsCase_IdAndCode(caseId, ACCESS_CODE))
                .thenReturn(Optional.of(pac));

        // When
        catchThrowable(() ->
                underTest.validateAndLinkParty(CASE_REFERENCE, ACCESS_CODE, userInfo));

        // Then
        verify(pcsCaseService, never()).save(caseEntity);
    }

    // Helper methods

    private PcsCaseEntity createCaseEntity(UUID caseId, UUID partyId, String linkedUserId) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(caseId);

        Defendant defendant = new Defendant();
        defendant.setPartyId(partyId);
        defendant.setLinkedUserId(linkedUserId);

        List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant);
        caseEntity.setDefendants(defendants);

        return caseEntity;
    }

    private PartyAccessCodeEntity createPartyAccessCodeEntity(UUID partyId) {
        PartyAccessCodeEntity pac = new PartyAccessCodeEntity();
        pac.setPartyId(partyId);
        pac.setCode(ACCESS_CODE);
        return pac;
    }

    private UserInfo createUserInfo(String userId) {
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn(userId);
        return userInfo;
    }

}
