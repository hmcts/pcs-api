package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimDraftService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private RespondPossessionClaimDraftService draftService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;

    private StartEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartEventHandler(
            pcsCaseService,
            addressMapper,
            draftService,
            securityContextService
        );
    }

    @Test
    void shouldBuildInitialResponseAndInitializeDraftWhenNoDraftExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postcode("SW1A 1AA")
            .build();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .address(addressEntity)
            .build();

        ClaimEntity claimEntity = createClaimWithDefendant(defendantEntity);
        PcsCaseEntity pcsCaseEntity = createCaseWithClaim(claimEntity);

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PCSCase initializedDraft = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                .party(Party.builder().firstName("John").lastName("Doe").build())
                .build())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressUK(addressEntity)).thenReturn(expectedAddress);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(draftService.initialize(eq(CASE_REFERENCE), any(PossessionClaimResponse.class), any(PCSCase.class)))
            .thenReturn(initializedDraft);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isNotNull();
        verify(draftService).initialize(eq(CASE_REFERENCE), any(PossessionClaimResponse.class), any(PCSCase.class));
    }

    @Test
    void shouldLoadExistingDraftWhenDraftAlreadyExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PCSCase existingDraft = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                .party(Party.builder()
                    .firstName("SavedName")
                    .emailAddress("saved@example.com")
                    .build())
                .build())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(true);
        when(draftService.load(eq(CASE_REFERENCE), any(PCSCase.class))).thenReturn(existingDraft);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isEqualTo(existingDraft);
        verify(draftService).load(eq(CASE_REFERENCE), any(PCSCase.class));
    }

    @Test
    void shouldUsePropertyAddressWhenAddressSameAsPropertyIsYes() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        AddressEntity propertyAddressEntity = AddressEntity.builder()
            .addressLine1("456 Property Street")
            .postTown("Manchester")
            .postcode("M1 1AA")
            .build();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("Jane")
            .lastName("Smith")
            .address(null)
            .addressSameAsProperty(VerticalYesNo.YES)
            .build();

        ClaimEntity claimEntity = createClaimWithDefendant(defendantEntity);
        PcsCaseEntity pcsCaseEntity = createCaseWithClaim(claimEntity);
        pcsCaseEntity.setPropertyAddress(propertyAddressEntity);

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PCSCase initializedDraft = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder().build())
            .build();

        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("456 Property Street")
            .postTown("Manchester")
            .postCode("M1 1AA")
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressUK(propertyAddressEntity)).thenReturn(propertyAddress);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(draftService.initialize(eq(CASE_REFERENCE), any(PossessionClaimResponse.class), any(PCSCase.class)))
            .thenReturn(initializedDraft);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(addressMapper).toAddressUK(propertyAddressEntity);
    }

    @Test
    void shouldUseDefendantAddressWhenAddressSameAsPropertyIsNotYes() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        AddressEntity defendantAddressEntity = AddressEntity.builder()
            .addressLine1("789 Defendant Street")
            .postTown("Birmingham")
            .postcode("B1 1AA")
            .build();

        AddressUK defendantAddress = AddressUK.builder()
            .addressLine1("789 Defendant Street")
            .postTown("Birmingham")
            .postCode("B1 1AA")
            .build();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("Bob")
            .lastName("Johnson")
            .address(defendantAddressEntity)
            .addressSameAsProperty(VerticalYesNo.NO)
            .build();

        ClaimEntity claimEntity = createClaimWithDefendant(defendantEntity);
        PcsCaseEntity pcsCaseEntity = createCaseWithClaim(claimEntity);

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PCSCase initializedDraft = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder().build())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressUK(defendantAddressEntity)).thenReturn(defendantAddress);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(draftService.initialize(eq(CASE_REFERENCE), any(PossessionClaimResponse.class), any(PCSCase.class)))
            .thenReturn(initializedDraft);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(addressMapper).toAddressUK(defendantAddressEntity);
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoClaimExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(Collections.emptyList())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);

        // When / Then
        assertThatThrownBy(() -> underTest.start(eventPayload))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No claim found for this case");
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

        PcsCaseEntity pcsCaseEntity = createCaseWithClaim(claimEntity);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);

        // When / Then
        assertThatThrownBy(() -> underTest.start(eventPayload))
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

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .build();

        ClaimEntity claimEntity = createClaimWithDefendant(defendantEntity);
        PcsCaseEntity pcsCaseEntity = createCaseWithClaim(claimEntity);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);

        // When / Then
        assertThatThrownBy(() -> underTest.start(eventPayload))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldCreatePartyWithNullFieldsWhenDefendantDataIsNull() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName(null)
            .lastName(null)
            .address(null)
            .build();

        ClaimEntity claimEntity = createClaimWithDefendant(defendantEntity);
        PcsCaseEntity pcsCaseEntity = createCaseWithClaim(claimEntity);

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        AddressUK emptyAddress = AddressUK.builder().build();

        PCSCase initializedDraft = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                .party(Party.builder().build())
                .build())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressUK(null)).thenReturn(emptyAddress);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(draftService.initialize(eq(CASE_REFERENCE), any(PossessionClaimResponse.class), any(PCSCase.class)))
            .thenReturn(initializedDraft);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty()).isNotNull();
    }

    private EventPayload<PCSCase, State> createEventPayload() {
        PCSCase caseData = PCSCase.builder().build();
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }

    private ClaimEntity createClaimWithDefendant(PartyEntity defendant) {
        ClaimEntity claimEntity = ClaimEntity.builder().build();
        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder()
            .party(defendant)
            .role(PartyRole.DEFENDANT)
            .build();
        claimEntity.getClaimParties().add(claimPartyEntity);
        return claimEntity;
    }

    private PcsCaseEntity createCaseWithClaim(ClaimEntity claim) {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        pcsCaseEntity.getClaims().add(claim);
        return pcsCaseEntity;
    }
}
