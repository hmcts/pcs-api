package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.StartEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.SubmitEventHandler;
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
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private PossessionClaimResponseMapper responseMapper;

    @Mock
    private DefendantAccessValidator accessValidator;

    @Mock
    private ImmutablePartyFieldValidator immutableFieldValidator;

    @Mock
    private uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository defendantResponseRepository;

    @BeforeEach
    void setUp() {
        // Create handlers with real dependencies
        StartEventHandler startEventHandler = new StartEventHandler(
            pcsCaseService,
            securityContextService,
            accessValidator,
            responseMapper,
            draftCaseDataService
        );

        SubmitEventHandler submitEventHandler = new SubmitEventHandler(
            draftCaseDataService,
            immutableFieldValidator,
            pcsCaseService,
            securityContextService,
            addressMapper,
            defendantResponseRepository
        );

        setEventUnderTest(new RespondPossessionClaim(
            startEventHandler,
            submitEventHandler
        ));

        // Mock existing draft with claimantProvided for save operations
        setupDefaultExistingDraft();
    }

    private void setupDefaultExistingDraft() {
        PossessionClaimResponse defaultResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().build())
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        PCSCase defaultExistingDraft = PCSCase.builder()
            .possessionClaimResponse(defaultResponse)
            .build();

        lenient().when(draftCaseDataService.getUnsubmittedCaseData(
                eq(TEST_CASE_REFERENCE), eq(EventId.respondPossessionClaim)))
            .thenReturn(Optional.of(defaultExistingDraft));
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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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

        PossessionClaimResponse mockResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(Party.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .address(expectedAddress)
                    .build())
                .build())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenReturn(matchingDefendant);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(matchingDefendant))).thenReturn(mockResponse);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false); // No draft exists yet - should seed

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getPossessionClaimResponse()).isNotNull();
        DefendantContactDetails contactDetails = result.getPossessionClaimResponse().getDefendantContactDetails();
        assertThat(contactDetails.getParty()).isNotNull();
        assertThat(contactDetails.getParty().getFirstName()).isEqualTo("John");
        assertThat(contactDetails.getParty().getLastName()).isEqualTo("Doe");
        assertThat(contactDetails.getParty().getAddress()).isEqualTo(expectedAddress);

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoDefendantsFound() {
        UUID defendantUserId = UUID.randomUUID();
        ClaimEntity claimEntity = ClaimEntity.builder()
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        pcsCaseEntity.getClaims().add(claimEntity);

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenThrow(new CaseAccessException("No defendants associated with this case"));

        PCSCase caseData = PCSCase.builder().build();

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoClaimExists() {
        UUID defendantUserId = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(Collections.emptyList())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenThrow(new CaseAccessException("No claim found for this case"));

        PCSCase caseData = PCSCase.builder().build();

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No claim found for this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenUserIsNotDefendant() {
        UUID defendantUserId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

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

        when(securityContextService.getCurrentUserId()).thenReturn(differentUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, differentUserId))
            .thenThrow(new CaseAccessException("User is not linked as a defendant on this case"));

        PCSCase caseData = PCSCase.builder().build();

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldSaveDraftWhenSubmitDraftIsYes() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(null)
                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.YES)
            .build();


        callSubmitHandler(caseData);

        verify(draftCaseDataService, times(1)).patchUnsubmittedEventData(
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
    void shouldSaveDraftWhenSubmitDraftAnswersIsNull() {
        // Given: submitDraftAnswers is null (defaults to NO = save draft)
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(null)
                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(null)
            .build();

        // When: submit callback is called
        callSubmitHandler(caseData);

        // Then: draft should be saved (null defaults to NO which means save draft)
        verify(draftCaseDataService, times(1)).patchUnsubmittedEventData(
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

        PossessionClaimResponse mockResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(Party.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .address(propertyAddress)
                    .build())
                .build())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenReturn(matchingDefendant);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(matchingDefendant))).thenReturn(mockResponse);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false); // No draft exists yet - should seed

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getPossessionClaimResponse()).isNotNull();
        DefendantContactDetails contactDetails = result.getPossessionClaimResponse().getDefendantContactDetails();
        assertThat(contactDetails.getParty()).isNotNull();
        assertThat(contactDetails.getParty().getFirstName()).isEqualTo("Jane");
        assertThat(contactDetails.getParty().getLastName()).isEqualTo("Smith");
        assertThat(contactDetails.getParty().getAddress()).isEqualTo(propertyAddress);
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

        AddressUK emptyAddress = AddressUK.builder()
            .addressLine1(null)
            .addressLine2(null)
            .addressLine3(null)
            .postTown(null)
            .county(null)
            .postCode(null)
            .country(null)
            .build();

        PossessionClaimResponse mockResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(Party.builder().address(emptyAddress).build())
                .build())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenReturn(matchingDefendant);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(matchingDefendant))).thenReturn(mockResponse);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(false); // No draft exists yet - should seed

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        // Party object should be created even when defendant has no data (firstName/lastName are null)
        // This is important for CCD event token validation
        assertThat(result.getPossessionClaimResponse()).isNotNull();
        DefendantContactDetails contactDetails = result.getPossessionClaimResponse().getDefendantContactDetails();
        assertThat(contactDetails.getParty()).isNotNull();
        assertThat(contactDetails.getParty().getFirstName()).isNull();
        assertThat(contactDetails.getParty().getLastName()).isNull();
        assertThat(contactDetails.getParty().getAddress()).isEqualTo(emptyAddress);

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

        // Mock draft data that user has already saved
        Party draftParty = Party.builder()
            .firstName("SavedFirstName")
            .lastName("SavedLastName")
            .emailAddress("saved@example.com")
            .build();

        PossessionClaimResponse draftResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(draftParty)
                .build())
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(draftResponse)
            .build();

        lenient().when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(true); // Draft already exists - should NOT seed
        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(Optional.of(draftData)); // Return saved draft data

        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        // Should return draft data (user's saved progress), NOT database defendant data
        assertThat(result.getPossessionClaimResponse()).isNotNull();
        DefendantContactDetails contactDetails = result.getPossessionClaimResponse().getDefendantContactDetails();
        assertThat(contactDetails.getParty()).isNotNull();
        assertThat(contactDetails.getParty().getFirstName()).isEqualTo("SavedFirstName");
        assertThat(contactDetails.getParty().getLastName()).isEqualTo("SavedLastName");
        assertThat(contactDetails.getParty().getEmailAddress()).isEqualTo("saved@example.com");

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

        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .address(address)
            .emailAddress("john.doe@example.com")
            .phoneNumber("07700900000")
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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
        // Note: submitDraftAnswers is NOT persisted - it's a transient UI control flag
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();

        DefendantContactDetails contactDetails = savedDraft.getPossessionClaimResponse().getDefendantContactDetails();
        assertThat(contactDetails.getParty()).isNotNull();
        assertThat(contactDetails.getParty().getFirstName()).isEqualTo("John");
        assertThat(contactDetails.getParty().getLastName()).isEqualTo("Doe");
        assertThat(contactDetails.getParty().getEmailAddress())
            .isEqualTo("john.doe@example.com");
        assertThat(contactDetails.getParty().getPhoneNumber())
            .isEqualTo("07700900000");
        assertThat(contactDetails.getParty().getAddress()).isNotNull();
        assertThat(contactDetails.getParty().getAddress().getAddressLine1())
            .isEqualTo("123 Test Street");
        assertThat(contactDetails.getParty().getAddress().getPostTown())
            .isEqualTo("London");
        assertThat(contactDetails.getParty().getAddress().getPostCode())
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
    void shouldReturnErrorWhenDefendantDataIsNull() {
        // Given: possessionClaimResponse exists but both defendantContactDetails and defendantResponses are null
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(null)  // No defendant contact details provided
            .defendantResponses(null)  // No defendant responses provided
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: submit callback is called
        var response = callSubmitHandler(caseData);

        // Then: should return error
        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0))
            .isEqualTo("Invalid submission: no data to save");

        // And: should NOT save draft
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldDefaultToNoAndSaveDraftWhenSubmitDraftAnswersIsNull() {
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(Party.builder().firstName("John").build())
                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(null)
            .build();

        var response = callSubmitHandler(caseData);

        // When submitDraftAnswers is null, defaults to NO and saves as draft
        assertThat(response.getErrors()).isNullOrEmpty();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldAllowNullFieldsInPartialUpdate() {
        // Given: party is null (valid for partial updates - preserves existing party info)
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(null)  // Null is valid - partial update preserves existing party
                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When: submit callback is called
        var response = callSubmitHandler(caseData);

        // Then: should succeed (null fields are valid in partial updates)
        assertThat(response.getErrors()).isNull();

        // And: draft should be saved via deep merge (preserves existing fields)
        verify(draftCaseDataService, times(1)).patchUnsubmittedEventData(
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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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

        DefendantContactDetails contactDetails = savedDraft.getPossessionClaimResponse().getDefendantContactDetails();
        assertThat(contactDetails.getParty()).isNotNull();
        assertThat(contactDetails.getParty().getFirstName()).isEqualTo("John");

        // Verify null fields were not set (remain null after deserialization)
        assertThat(contactDetails.getParty().getLastName()).isNull();
        assertThat(contactDetails.getParty().getEmailAddress()).isNull();
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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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
        Party savedParty = savedDraft.getPossessionClaimResponse()
            .getDefendantContactDetails().getParty();
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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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
        Party savedParty = savedDraft.getPossessionClaimResponse()
            .getDefendantContactDetails().getParty();
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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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
        Party savedParty = savedDraft.getPossessionClaimResponse()
            .getDefendantContactDetails().getParty();
        AddressUK savedAddress = savedParty.getAddress();

        // Non-null fields should be present
        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
        assertThat(savedParty.getEmailAddress()).isEqualTo("john@example.com");
        assertThat(savedAddress.getAddressLine1()).isEqualTo("123 Main Street");
        assertThat(savedAddress.getPostTown()).isEqualTo("London");
        assertThat(savedAddress.getPostCode()).isEqualTo("SW1A 1AA");

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
            .defendantContactDetails(DefendantContactDetails.builder()
                .party(party)
                .build())
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
        Party savedParty = savedDraft.getPossessionClaimResponse()
            .getDefendantContactDetails().getParty();

        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
        assertThat(savedParty.getAddress()).isNull();
    }

}

