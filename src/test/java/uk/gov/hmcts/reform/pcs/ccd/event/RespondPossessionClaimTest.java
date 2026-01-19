package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class RespondPossessionClaimTest extends BaseEventTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private AddressMapper addressMapper;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new RespondPossessionClaim(
            draftCaseDataService,
            pcsCaseService,
            securityContextService,
            addressMapper
        ));
    }

    @Test
    void shouldPatchUnsubmittedEventData() {
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        callSubmitHandler(caseData);

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );

    }

    @Test
    void shouldPopulatePossessionClaimResponseWhenUserIsMatchingDefendant() {
        UUID defendantUserId = UUID.randomUUID();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postcode("SW1A 1AA")
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
        when(addressMapper.toAddressUK(addressEntity)).thenReturn(expectedAddress);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false); // No draft exists yet - should seed

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getPossessionClaimResponse()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Doe");
        assertThat(result.getPossessionClaimResponse().getParty().getAddress()).isEqualTo(expectedAddress);

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoDefendantsFound() {
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

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoClaimExists() {
        UUID defendantUserId = UUID.randomUUID();
        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(Collections.emptyList())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No claim found for this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenUserIsNotDefendant() {
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

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldNotSaveDraftWhenSubmitDraftIsYes() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.YES)
            .build();


        callSubmitHandler(caseData);

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldNotSaveDraftWhenPossessionClaimResponseIsNull() {
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .submitDraftAnswers(YesOrNo.NO)
            .build();


        callSubmitHandler(caseData);

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldNotSaveDraftWhenSubmitDraftIsNull() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(null)
            .build();


        callSubmitHandler(caseData);

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldUsePropertyAddressWhenAddressSameAsPropertyIsYes() {
        UUID defendantUserId = UUID.randomUUID();

        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("456 Property Street")
            .postTown("Manchester")
            .postCode("M1 1AA")
            .build();

        AddressEntity propertyAddressEntity = AddressEntity.builder()
            .addressLine1("456 Property Street")
            .postTown("Manchester")
            .postcode("M1 1AA")
            .build();

        PartyEntity matchingDefendant = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("Jane")
            .lastName("Smith")
            .address(null)
            .addressSameAsProperty(VerticalYesNo.YES)
            .build();

        ClaimEntity claimEntity = ClaimEntity.builder()
            .build();

        ClaimPartyEntity claimPartyEntity = ClaimPartyEntity.builder()
            .party(matchingDefendant)
            .role(PartyRole.DEFENDANT)
            .build();
        claimEntity.getClaimParties().add(claimPartyEntity);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .propertyAddress(propertyAddressEntity)
            .build();
        pcsCaseEntity.getClaims().add(claimEntity);
        pcsCaseEntity.getParties().add(matchingDefendant);

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressUK(propertyAddressEntity)).thenReturn(propertyAddress);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false); // No draft exists yet - should seed

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getPossessionClaimResponse()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("Jane");
        assertThat(result.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Smith");
        assertThat(result.getPossessionClaimResponse().getParty().getAddress()).isEqualTo(propertyAddress);
    }

    @Test
    void shouldCreatePartyObjectEvenWhenDefendantHasNoData() {
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity matchingDefendant = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName(null)
            .lastName(null)
            .address(null)
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

        AddressUK emptyAddress = AddressUK.builder()
            .addressLine1(null)
            .addressLine2(null)
            .addressLine3(null)
            .postTown(null)
            .county(null)
            .postCode(null)
            .country(null)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(addressMapper.toAddressUK(null)).thenReturn(emptyAddress);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false); // No draft exists yet - should seed

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getPossessionClaimResponse()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isNull();
        assertThat(result.getPossessionClaimResponse().getParty().getLastName()).isNull();
        // Address should be an AddressUK object with all fields null (for CCD token validation)
        assertThat(result.getPossessionClaimResponse().getParty().getAddress()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty().getAddress().getAddressLine1()).isNull();
        assertThat(result.getPossessionClaimResponse().getParty().getAddress().getPostTown()).isNull();
        assertThat(result.getPossessionClaimResponse().getParty().getAddress().getPostCode()).isNull();
        assertThat(result.getPossessionClaimResponse().getParty().getAddress().getCountry()).isNull();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldNotOverwriteDraftWhenDraftAlreadyExists() {
        UUID defendantUserId = UUID.randomUUID();

        AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postcode("SW1A 1AA")
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
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(true); // Draft already exists - should NOT seed

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        // Should still return the party data for CCD token validation
        assertThat(result.getPossessionClaimResponse()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(result.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Doe");

        // Should NOT call patchUnsubmittedEventData when draft already exists
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldSaveCompletePartyDataInDraftIncludingContactDetails() {
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        uk.gov.hmcts.reform.pcs.ccd.domain.Party party = uk.gov.hmcts.reform.pcs.ccd.domain.Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .emailAddress("john.doe@example.com")
            .phoneNumber("07700900000")
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        callSubmitHandler(caseData);

        ArgumentCaptor<PCSCase> draftCaptor = forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            draftCaptor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PCSCase savedDraft = draftCaptor.getValue();
        assertThat(savedDraft.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getContactByPhone()).isEqualTo(YesOrNo.YES);
        assertThat(savedDraft.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getLastName()).isEqualTo("Doe");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getEmailAddress())
            .isEqualTo("john.doe@example.com");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getPhoneNumber()).isEqualTo("07700900000");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getAddress()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getAddress().getAddressLine1())
            .isEqualTo("123 Test Street");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getAddress().getPostTown())
            .isEqualTo("London");
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getAddress().getPostCode())
            .isEqualTo("SW1A 1AA");
    }

    @Test
    void shouldReturnErrorWhenPossessionClaimResponseIsNull() {
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0)).isEqualTo("Invalid submission: missing response data");

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldReturnErrorWhenSubmitDraftAnswersIsNull() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .party(Party.builder().firstName("John").build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(null)
            .build();

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0)).isEqualTo("Invalid submission: missing submit flag");

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldReturnErrorWhenPartyIsNull() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .party(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0))
            .isEqualTo("Invalid response structure. Please refresh the page and try again.");

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldReturnErrorWhenDraftSaveThrowsException() {
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        doThrow(new RuntimeException("Database connection failed"))
            .when(draftCaseDataService).patchUnsubmittedEventData(
                eq(TEST_CASE_REFERENCE),
                any(PCSCase.class),
                eq(EventId.respondPossessionClaim)
            );

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0))
            .isEqualTo("We couldn't save your response. Please try again or contact support.");
    }

    @Test
    void shouldReturnDefaultResponseWhenDraftSavedSuccessfully() {
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldOmitAllNullFieldsFromPartyWhenSerializing() throws Exception {
        // Given: Party with all fields null except firstName
        Party party = Party.builder()
            .firstName("John")
            .lastName(null)
            .orgName(null)
            .nameKnown(null)
            .emailAddress(null)
            .address(null)
            .addressKnown(null)
            .addressSameAsProperty(null)
            .phoneNumber(null)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: Submitting draft
        callSubmitHandler(caseData);

        // Then: Verify the saved draft omits null fields
        ArgumentCaptor<PCSCase> captor = forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            captor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PCSCase savedDraft = captor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getFirstName()).isEqualTo("John");

        // Verify null fields were not set (remain null after deserialization)
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getLastName()).isNull();
        assertThat(savedDraft.getPossessionClaimResponse().getParty().getEmailAddress()).isNull();
    }

    @Test
    void shouldOmitNullAddressFieldsWhenSerializing() throws Exception {
        // Given: Address with some fields null
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Main Street")
            .addressLine2(null)
            .addressLine3(null)
            .postTown("London")
            .county(null)
            .postCode("SW1A 1AA")
            .country(null)
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: Submitting draft
        callSubmitHandler(caseData);

        // Then: Verify the saved draft omits null address fields
        ArgumentCaptor<PCSCase> captor = forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            captor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PCSCase savedDraft = captor.getValue();
        Party savedParty = savedDraft.getPossessionClaimResponse().getParty();
        AddressUK savedAddress = savedParty.getAddress();

        assertThat(savedAddress).isNotNull();
        assertThat(savedAddress.getAddressLine1()).isEqualTo("123 Main Street");
        assertThat(savedAddress.getPostTown()).isEqualTo("London");
        assertThat(savedAddress.getPostCode()).isEqualTo("SW1A 1AA");

        // Verify null fields were not set
        assertThat(savedAddress.getAddressLine2()).isNull();
        assertThat(savedAddress.getCounty()).isNull();
    }

    @Test
    void shouldOmitNullContactByPhoneField() throws Exception {
        // Given: Response with null contactByPhone
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(null)  // Null field
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: Submitting draft
        callSubmitHandler(caseData);

        // Then: Verify contactByPhone was omitted
        ArgumentCaptor<PCSCase> captor = forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            captor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PCSCase savedDraft = captor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getContactByPhone()).isNull();
    }

    @Test
    void shouldIncludeAllNonNullFieldsWhenSerializing() throws Exception {
        // Given: Fully populated response with no nulls
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Main Street")
            .addressLine2("Apt 4B")
            .addressLine3("Building C")
            .postTown("London")
            .county("Greater London")
            .postCode("SW1A 1AA")
            .country("UK")
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .orgName("Test Org")
            .nameKnown(VerticalYesNo.YES)
            .emailAddress("john@example.com")
            .address(address)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .phoneNumber("07700900000")
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: Submitting draft
        callSubmitHandler(caseData);

        // Then: All fields should be included
        ArgumentCaptor<PCSCase> captor = forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            captor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PCSCase savedDraft = captor.getValue();
        Party savedParty = savedDraft.getPossessionClaimResponse().getParty();
        AddressUK savedAddress = savedParty.getAddress();

        // Verify all party fields are preserved
        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
        assertThat(savedParty.getOrgName()).isEqualTo("Test Org");
        assertThat(savedParty.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedParty.getEmailAddress()).isEqualTo("john@example.com");
        assertThat(savedParty.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07700900000");

        // Verify all address fields are preserved
        assertThat(savedAddress.getAddressLine1()).isEqualTo("123 Main Street");
        assertThat(savedAddress.getAddressLine2()).isEqualTo("Apt 4B");
        assertThat(savedAddress.getAddressLine3()).isEqualTo("Building C");
        assertThat(savedAddress.getPostTown()).isEqualTo("London");
        assertThat(savedAddress.getCounty()).isEqualTo("Greater London");
        assertThat(savedAddress.getPostCode()).isEqualTo("SW1A 1AA");
        assertThat(savedAddress.getCountry()).isEqualTo("UK");

        assertThat(savedDraft.getPossessionClaimResponse().getContactByPhone()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldHandleMixedNullAndNonNullPartyFields() throws Exception {
        // Given: Party with mixed null and non-null fields (realistic scenario)
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Main Street")
            .addressLine2(null)  // User didn't fill this
            .postTown("London")
            .postCode("SW1A 1AA")
            .country(null)  // User didn't fill this
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .orgName(null)  // Not applicable for individual
            .emailAddress("john@example.com")
            .address(address)
            .phoneNumber(null)  // User didn't provide
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.NO)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: Submitting draft
        callSubmitHandler(caseData);

        // Then: Non-null fields included, null fields omitted
        ArgumentCaptor<PCSCase> captor = forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            captor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PCSCase savedDraft = captor.getValue();
        Party savedParty = savedDraft.getPossessionClaimResponse().getParty();
        AddressUK savedAddress = savedParty.getAddress();

        // Non-null fields should be present
        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
        assertThat(savedParty.getEmailAddress()).isEqualTo("john@example.com");
        assertThat(savedAddress.getAddressLine1()).isEqualTo("123 Main Street");
        assertThat(savedAddress.getPostTown()).isEqualTo("London");
        assertThat(savedAddress.getPostCode()).isEqualTo("SW1A 1AA");
        assertThat(savedDraft.getPossessionClaimResponse().getContactByPhone()).isEqualTo(YesOrNo.NO);

        // Null fields should remain null (not overwritten)
        assertThat(savedParty.getOrgName()).isNull();
        assertThat(savedParty.getPhoneNumber()).isNull();
        assertThat(savedAddress.getAddressLine2()).isNull();
        assertThat(savedAddress.getCountry()).isNull();
    }

    @Test
    void shouldOmitNullAddressWhenPartyAddressIsNull() throws Exception {
        // Given: Party with null address
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(null)  // No address provided
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: Submitting draft
        callSubmitHandler(caseData);

        // Then: Address field should be omitted
        ArgumentCaptor<PCSCase> captor = forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            captor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PCSCase savedDraft = captor.getValue();
        Party savedParty = savedDraft.getPossessionClaimResponse().getParty();

        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
        assertThat(savedParty.getAddress()).isNull();
    }

}

