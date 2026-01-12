package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitDefendantResponseTest extends BaseEventTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new SubmitDefendantResponse(draftCaseDataService, pcsCaseService, securityContextService, modelMapper));
    }

    @Test
    void shouldPatchUnsubmittedEventData() {
        // Given Defendant response
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        UserInfo userInfo = UserInfo.builder()
            .uid(UUID.randomUUID().toString())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        callSubmitHandler(caseData);

        //Then - Verify filtered PCSCase (only defendantResponse) is saved
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.submitDefendantResponse),
            any(UUID.class)
        );

    }

    @Test
    void shouldPopulateDefendantResponseWhenUserIsMatchingDefendant() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        PartyEntity matchingDefendant = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .address(addressEntity)
            .build();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .build();

        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder()
            .party(matchingDefendant)
            .role(PartyRole.DEFENDANT)
            .build();
        claimEntity.getClaimParties().add(claimPartyEntity);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        pcsCaseEntity.getClaims().add(claimEntity);
        pcsCaseEntity.getParties().add(matchingDefendant);

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(modelMapper.map(addressEntity, AddressUK.class)).thenReturn(expectedAddress);

        PCSCase caseData = PCSCase.builder().build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getDefendantResponse()).isNotNull();
        assertThat(result.getDefendantResponse().getParty()).isNotNull();
        assertThat(result.getDefendantResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getDefendantResponse().getParty().getLastName()).isEqualTo("Doe");
        assertThat(result.getDefendantResponse().getParty().getAddress()).isEqualTo(expectedAddress);

        // Verify draft is created in start() with filtered PCSCase (only defendantResponse)
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.submitDefendantResponse),
            eq(defendantUserId)
        );
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoDefendantsFound() {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        pcsCaseEntity.getClaims().add(claimEntity);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        // When/Then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenDefendantsIsNull() {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(Collections.emptySet())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        // When/Then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenUserIsNotDefendant() {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(differentUserId.toString())
            .build();

        PartyEntity matchingDefendant = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .build();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .build();

        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder()
            .party(matchingDefendant)
            .role(PartyRole.DEFENDANT)
            .build();
        claimEntity.getClaimParties().add(claimPartyEntity);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        pcsCaseEntity.getClaims().add(claimEntity);
        pcsCaseEntity.getParties().add(matchingDefendant);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        // When/Then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldNotSaveDraftWhenSubmitDraftIsYes() {
        // Given Defendant response
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        UserInfo userInfo = UserInfo.builder()
            .uid(UUID.randomUUID().toString())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        callSubmitHandler(caseData);

        //Then
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.submitDefendantResponse),
            any(UUID.class)
        );
    }

    @Test
    void shouldNotSaveDraftWhenDefendantResponseIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .defendantResponse(null)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        UserInfo userInfo = UserInfo.builder()
            .uid(UUID.randomUUID().toString())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        callSubmitHandler(caseData);

        //Then
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.submitDefendantResponse),
            any(UUID.class)
        );
    }

    @Test
    void shouldNotSaveDraftWhenSubmitDraftIsNull() {
        // Given Defendant response
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(null)
            .build();

        UserInfo userInfo = UserInfo.builder()
            .uid(UUID.randomUUID().toString())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        callSubmitHandler(caseData);

        //Then
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.submitDefendantResponse),
            any(UUID.class)
        );
    }

}
