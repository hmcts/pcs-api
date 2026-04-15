package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.StartEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.SubmitEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespondPossessionClaimTest extends BaseEventTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private ClaimResponseService claimResponseService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private PossessionClaimResponseMapper responseMapper;

    @Mock
    private DefendantAccessValidator accessValidator;

    @Mock
    private DefendantResponseService defendantResponseService;

    @Mock
    private RespondToPossessionDraftSavePage respondToPossessionDraftSavePage;

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
            claimResponseService,
            defendantResponseService
        );

        setEventUnderTest(new RespondPossessionClaim(
            startEventHandler,
            submitEventHandler,
            respondToPossessionDraftSavePage
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

        verify(draftCaseDataService).hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim);

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
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, differentUserId))
            .thenThrow(new CaseAccessException("User is not linked as a defendant on this case"));

        PCSCase caseData = PCSCase.builder().build();

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }


    @Test
    void shouldNotSaveDraftWhenPossessionClaimResponseIsNull() {
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
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

        verify(draftCaseDataService).hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim);
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

        verify(draftCaseDataService).hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim);

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

        verify(draftCaseDataService).hasUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim);
        verify(draftCaseDataService).getUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim);

        // Should NOT call patchUnsubmittedEventData when draft already exists
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldReturnErrorWhenPossessionClaimResponseIsNull() {
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .build();

        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(Optional.of(caseData));

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().getFirst()).isEqualTo("Invalid submission: missing response data");

        verify(draftCaseDataService).getUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim);

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }
}

