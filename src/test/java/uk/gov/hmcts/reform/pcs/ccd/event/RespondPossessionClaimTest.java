package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.CitizenCaseDraftLoader;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.LegalRepresentativeCaseDraftLoader;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.StartEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.SubmitEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

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

    @Mock
    private SelectedPartyRetriever selectedPartyRetriever;

    @Mock
    private UserInfo userInfo;

    @Mock
    private DefendantResponseRepository defendantResponseRepository;

    @Mock
    private LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;

    @BeforeEach
    void setUp() {

        // Create handlers with real dependencies
        StartEventHandler startEventHandler = new StartEventHandler(
            securityContextService,
            new CitizenCaseDraftLoader(pcsCaseService, securityContextService, accessValidator, responseMapper,
                                       draftCaseDataService),
            new LegalRepresentativeCaseDraftLoader(pcsCaseService, responseMapper, draftCaseDataService,
                                                   defendantResponseRepository, legalRepForDefendantAccessValidator,
                                                   securityContextService, selectedPartyRetriever)
        );

        SubmitEventHandler submitEventHandler = new SubmitEventHandler(
            draftCaseDataService,
            claimResponseService,
            defendantResponseService,
            securityContextService,
            selectedPartyRetriever
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
    void shouldPopulatePossessionClaimResponseWhenUserIsMatchingDefendant_ForCitizenUser() {
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

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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
    void shouldThrowCaseAccessExceptionWhenNoDefendantsFound_ForCitizenUser() {
        UUID defendantUserId = UUID.randomUUID();
        ClaimEntity claimEntity = ClaimEntity.builder()
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .build();
        pcsCaseEntity.getClaims().add(claimEntity);

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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
    void shouldThrowCaseAccessExceptionWhenNoClaimExists_ForCitizenUser() {
        UUID defendantUserId = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(Collections.emptyList())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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
    void shouldThrowCaseAccessExceptionWhenUserIsNotDefendant_ForCitizenUser() {
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

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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
    void shouldNotSaveDraftWhenPossessionClaimResponseIsNull_ForCitizenUser() {
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));

        callSubmitHandler(caseData);

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }


    @Test
    void shouldUsePropertyAddressWhenAddressSameAsPropertyIsYes_ForCitizenUser() {
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

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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
    void shouldCreatePartyObjectEvenWhenDefendantHasNoData_ForCitizenUser() {
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

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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
    void shouldNotOverwriteDraftWhenDraftAlreadyExists_ForCitizenUser() {
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

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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
    void shouldReturnErrorWhenPossessionClaimResponseIsNull_ForCitizenUser() {
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
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

    @Test
    void shouldReturnErrorWhenDefendantResponseIsNull_ForCitizenUser() {
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                                         .defendantResponses(null)
                                         .build())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim))
            .thenReturn(Optional.of(caseData));

        var response = callSubmitHandler(caseData);

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().getFirst()).isEqualTo("Invalid submission: missing defendant response data");

        verify(draftCaseDataService).getUnsubmittedCaseData(TEST_CASE_REFERENCE, EventId.respondPossessionClaim);

        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.respondPossessionClaim)
        );
    }

    @Test
    void shouldReturnRepresentedPartiesOnlyWhenNoPartyContextProvided_ForLegalRepresentativeUser() {
        // given
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();
        UUID differentPartyId = UUID.randomUUID();

        PCSCase caseData = PCSCase.builder().build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder()
            .id(representedPartyId)
            .firstName("Sam")
            .lastName("Defendant")
            .build();

        PartyEntity representedParty2 = PartyEntity.builder()
            .id(differentPartyId)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty, representedParty2));
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));

        // when
        PCSCase result = callStartHandler(caseData);

        // then
        assertThat(result.getPossessionClaimResponse()).isNull();
        assertThat(result.getParties()).hasSize(2);
        Party returnedParty = result.getParties().getFirst().getValue();
        assertThat(result.getParties().getFirst().getId()).isEqualTo(representedPartyId.toString());
        assertThat(returnedParty.getFirstName()).isEqualTo("Sam");
        assertThat(returnedParty.getLastName()).isEqualTo("Defendant");
        verify(draftCaseDataService, never()).hasUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                                     representedPartyId);
    }

    @Test
    void shouldInitializeDraftForSelectedRepresentedPartyWhenNoDraftExists_ForLegalRepresentativeUser() {
        // given
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();
        UUID differentPartyId = UUID.randomUUID();

        PCSCase caseData = PCSCase.builder().build();
        PossessionClaimResponse response = PossessionClaimResponse.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();
        PartyEntity representedParty2 = PartyEntity.builder().id(differentPartyId).build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        when(selectedPartyRetriever.getSelectedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(responseMapper.mapFrom(caseData, representedParty)).thenReturn(response);
        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty, representedParty2));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(TEST_CASE_REFERENCE,
                                                                                     representedPartyId))
            .thenReturn(false);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                         representedPartyId))
            .thenReturn(false);

        // when
        PCSCase result = callStartHandler(caseData);

        // then
        assertThat(result.getPossessionClaimResponse()).isEqualTo(response);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim), eq(representedPartyId)
        );
    }

    @Test
    void shouldLoadDraftForSelectedRepresentedPartyWhenDraftExists_ForLegalRepresentativeUser() {
        // given
        UUID representedPartyId = UUID.randomUUID();
        UUID differentPartyId = UUID.randomUUID();

        Party party = Party.builder()
            .build();
        Party party2 = Party.builder()
            .build();

        List<ListValue<Party>> defendantList = new ArrayList<>();
        defendantList.add(ListValue.<Party>builder().value(party).id(differentPartyId.toString()).build());
        defendantList.add(ListValue.<Party>builder().value(party2).id(representedPartyId.toString()).build());

        PCSCase caseData = PCSCase.builder()
            .allDefendants(defendantList)
            .build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();
        PartyEntity representedParty2 = PartyEntity.builder().id(differentPartyId).build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        UUID legalRepUserId = UUID.randomUUID();
        PossessionClaimResponse savedResponse = PossessionClaimResponse.builder().build();
        PCSCase savedDraft = PCSCase.builder()
            .possessionClaimResponse(savedResponse)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        when(selectedPartyRetriever.getSelectedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                         representedPartyId))
            .thenReturn(Optional.of(savedDraft));
        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty, representedParty2));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(TEST_CASE_REFERENCE,
                                                                                     representedPartyId))
            .thenReturn(false);
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                         representedPartyId))
            .thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                         representedPartyId))
            .thenReturn(Optional.of(savedDraft));
        when(responseMapper.buildPartyFromEntity(representedParty, caseData))
            .thenReturn(uk.gov.hmcts.reform.pcs.ccd.domain.Party.builder().build());

        // when
        PCSCase result = callStartHandler(caseData);

        // then
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim), eq(representedPartyId)
        );
    }

    @Test
    void shouldRejectPartyOutsideRepresentedDefendants_ForLegalRepresentativeUser() {
        // given
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();
        UUID differentPartyId = UUID.randomUUID();
        UUID thirdPartyId = UUID.randomUUID();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();
        PartyEntity representedParty2 = PartyEntity.builder().id(thirdPartyId).build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        PCSCase caseData = PCSCase.builder()
            .build();
        when(selectedPartyRetriever.getSelectedPartyId(caseData)).thenReturn(Optional.of(differentPartyId));
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty, representedParty2));

        // when / then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldRejectSelectedPartyWhenResponseAlreadySubmitted_ForLegalRepresentativeUser() {
        // given
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();
        UUID differentPartyId = UUID.randomUUID();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();
        PartyEntity representedParty2 = PartyEntity.builder().id(differentPartyId).build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        PCSCase caseData = PCSCase.builder()
            .build();
        when(selectedPartyRetriever.getSelectedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty, representedParty2));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(TEST_CASE_REFERENCE,
                                                                                     representedPartyId))
            .thenReturn(true);

        // when / then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("A response has already been submitted for this case.");
    }

    @Test
    void shouldInitializeDraftForSingleRepresentedPartyWhenNoDraftExists_ForLegalRepresentativeUser() {
        // given
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();

        PCSCase caseData = PCSCase.builder().build();
        PossessionClaimResponse response = PossessionClaimResponse.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        when(responseMapper.mapFrom(caseData, representedParty)).thenReturn(response);
        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty));
        when(draftCaseDataService.hasUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                         representedPartyId))
            .thenReturn(false);

        // when
        PCSCase result = callStartHandler(caseData);

        // then
        assertThat(result.getPossessionClaimResponse()).isEqualTo(response);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim), eq(representedPartyId)
        );
        verify(selectedPartyRetriever, never()).getSelectedPartyId(any(PCSCase.class));
        verify(defendantResponseRepository, never()).existsByClaimPcsCaseCaseReferenceAndPartyId(TEST_CASE_REFERENCE,
                                                                                                 representedPartyId);
    }

    @Test
    void shouldSubmitLegalRepresentativeDraftForSelectedParty_ForLegalRepresentativeUser() {
        // given
        UUID representedPartyId = UUID.randomUUID();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        when(selectedPartyRetriever.getSelectedPartyId(caseData)).thenReturn(Optional.of(representedPartyId));
        when(draftCaseDataService.getUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                         representedPartyId))
            .thenReturn(Optional.of(caseData));

        // when
        var response = callSubmitHandler(caseData);

        // then
        assertThat(response.getErrors()).isNullOrEmpty();
        verify(claimResponseService).saveDraftDataForParty(possessionClaimResponse, TEST_CASE_REFERENCE,
                                                           representedPartyId);
        verify(defendantResponseService).saveDefendantResponse(TEST_CASE_REFERENCE, possessionClaimResponse,
                                                               representedPartyId);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim,
                                                               representedPartyId);
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(TEST_CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowExceptionForNoSelectedParty_ForLegalRepresentativeUser() {
        // given
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        when(selectedPartyRetriever.getSelectedPartyId(caseData)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> callSubmitHandler(caseData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No selected responding party id for respond to claim");
    }
}

