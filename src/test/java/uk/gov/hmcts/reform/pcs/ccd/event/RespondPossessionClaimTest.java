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
import uk.gov.hmcts.reform.pcs.ccd.domain.draft.patch.PcsDraftPatch;
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
            any(PcsDraftPatch.class),
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

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

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

        ArgumentCaptor<PcsDraftPatch> draftCaptor = forClass(PcsDraftPatch.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            draftCaptor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PcsDraftPatch savedDraft = draftCaptor.getValue();
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
                any(PcsDraftPatch.class),
                eq(EventId.respondPossessionClaim)
            );

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0))
            .isEqualTo("We couldn't save your response. Please try again or contact support.");
    }

    @Test
    void shouldSerializePcsDraftPatchOmittingNullFields() throws Exception {
        // Given: A party with some fields populated and some null
        AddressUK address = AddressUK.builder()
            .addressLine1(null)  // null - should be omitted
            .postTown("London")   // non-null - should be present
            .postCode("SW1A 1AA") // non-null - should be present
            .country("UK")        // non-null - should be present
            .build();

        Party party = Party.builder()
            .firstName("John")
            .lastName(null)      // null - should be omitted
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

        // When: Submitting draft
        callSubmitHandler(caseData);

        // Then: Capture the PcsDraftPatch
        ArgumentCaptor<PcsDraftPatch> draftCaptor = forClass(PcsDraftPatch.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            draftCaptor.capture(),
            eq(EventId.respondPossessionClaim)
        );

        PcsDraftPatch capturedPatch = draftCaptor.getValue();

        // Serialize the PcsDraftPatch to JSON using Jackson
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = objectMapper.writeValueAsString(capturedPatch);

        // Assert: JSON contains non-null fields
        assertThat(json)
            .contains("\"Country\":\"UK\"")       // Country was set
            .contains("\"PostTown\":\"London\"")  // PostTown was set
            .contains("\"PostCode\":\"SW1A 1AA\"") // PostCode was set
            .contains("\"firstName\":\"John\"");   // firstName was set

        // Assert: JSON does NOT contain null fields (they are omitted)
        assertThat(json)
            .doesNotContain("\"AddressLine1\"")   // AddressLine1 was null
            .doesNotContain("\"lastName\"");       // lastName was null
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
            any(PcsDraftPatch.class),
            eq(EventId.respondPossessionClaim)
        );
    }

}

