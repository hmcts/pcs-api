package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.LegalRepPartySelectionService;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.PossessionClaimMerger;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepStartEventStrategyTest {

    private static final long CASE_REFERENCE = 12345L;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;

    @Mock
    private LegalRepPartySelectionService legalRepPartySelectionService;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private TenancyLicenceView tenancyLicenceView;

    @Mock
    private NoticeOfPossessionView noticeOfPossessionView;

    @Mock
    private RentArrearsView rentArrearsView;

    @Mock
    private ClaimPartyOrganisationRepository claimPartyOrganisationRepository;

    @InjectMocks
    private LegalRepStartEventStrategy underTest;

    @Test
    void shouldSupportNonCitizenRoles() {
        // given
        List<String> roles = List.of(UserRole.CITIZEN.getRole());

        // when
        boolean result = underTest.supports(roles);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldSupportLegalRepRoles() {
        // given
        List<String> roles = List.of(UserRole.DEFENDANT_SOLICITOR.getRole());

        // when
        boolean result = underTest.supports(roles);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldLoadDraft_ForSingleDefendant() {
        // given
        PCSCase pcsCase = mock(PCSCase.class);

        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        PartyEntity defendant = mock(PartyEntity.class);

        String organisationId = "org";
        List<PartyEntity> defendants = List.of(defendant);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, organisationId))
            .thenReturn(defendants);
        when(legalRepPartySelectionService.filterDefendantsAwaitingResponse(CASE_REFERENCE, defendants))
            .thenReturn(defendants);

        when(legalRepPartySelectionService.getDraftCaseData(CASE_REFERENCE, pcsCase,
                                                            defendant, defendants, organisationId))
            .thenReturn(pcsCase);

        // when
        PCSCase result = underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // then
        assertThat(result).isEqualTo(pcsCase);

        verify(legalRepPartySelectionService).getDraftCaseData(CASE_REFERENCE, pcsCase, defendant, defendants,
                                                               organisationId);
        verify(tenancyLicenceView).setCaseFields(pcsCase, caseEntity);
        verify(noticeOfPossessionView).setCaseFields(pcsCase, caseEntity);
        verify(rentArrearsView).setCaseFields(pcsCase, caseEntity);
    }

    @Test
    void shouldLoadDraft_ForMultipleDefendants() {
        // given
        PCSCase pcsCase = mock(PCSCase.class);

        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        PartyEntity defendant1 = mock(PartyEntity.class);
        PartyEntity defendant2 = mock(PartyEntity.class);

        String organisationId = "org";
        List<PartyEntity> defendants = List.of(defendant1, defendant2);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, organisationId))
            .thenReturn(defendants);
        when(legalRepPartySelectionService.filterDefendantsAwaitingResponse(CASE_REFERENCE, defendants))
            .thenReturn(defendants);

        when(legalRepPartySelectionService.getDraft(pcsCase, defendants, CASE_REFERENCE, organisationId))
            .thenReturn(pcsCase);

        // when
        PCSCase result = underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // then
        assertThat(result).isEqualTo(pcsCase);

        verify(legalRepPartySelectionService).getDraft(pcsCase, defendants, CASE_REFERENCE, organisationId);
    }

    @Test
    void shouldBuildSubmittedResponseWhenSelectedPartyHasAlreadySubmitted() {
        // Given
        PartyEntity respondedDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity awaitingDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        List<PartyEntity> representedDefendants = List.of(respondedDefendant, awaitingDefendant);
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        String organisationId = "org";

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, organisationId))
            .thenReturn(representedDefendants);
        when(legalRepPartySelectionService.filterDefendantsAwaitingResponse(CASE_REFERENCE, representedDefendants))
            .thenReturn(List.of(awaitingDefendant));
        when(legalRepPartySelectionService.hasSubmittedResponseForCurrentlySelectedParty(CASE_REFERENCE))
            .thenReturn(true);
        when(legalRepPartySelectionService.buildSubmittedResponseCase(pcsCase, representedDefendants))
            .thenReturn(pcsCase);

        // When
        underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // Then
        verify(legalRepPartySelectionService).buildSubmittedResponseCase(pcsCase, representedDefendants);
    }

    @Test
    void shouldBuildSubmittedResponseWithRepresentedDefendantsWhenAllHaveRespondedAndNoPartySelected() {
        // Given
        PartyEntity respondedDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        List<PartyEntity> representedDefendants = List.of(respondedDefendant);
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        String organisationId = "org";

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, organisationId))
            .thenReturn(representedDefendants);
        when(legalRepPartySelectionService.filterDefendantsAwaitingResponse(CASE_REFERENCE, representedDefendants))
            .thenReturn(List.of());
        when(legalRepPartySelectionService.buildSubmittedResponseCase(pcsCase, representedDefendants))
            .thenReturn(pcsCase);

        // When
        PCSCase result = underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // Then
        assertThat(result).isEqualTo(pcsCase);
        verify(legalRepPartySelectionService).buildSubmittedResponseCase(pcsCase, representedDefendants);
        verify(legalRepPartySelectionService, never())
            .hasSubmittedResponseForCurrentlySelectedParty(CASE_REFERENCE);
        verify(legalRepPartySelectionService, never())
            .getDraftCaseData(anyLong(), any(), any(), anyList(), anyString());
    }

    @Test
    void shouldLoadDraftForRemainingDefendantWhenAnotherRepresentedDefendantHasResponded() {
        // Given
        PartyEntity respondedDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity awaitingDefendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        List<PartyEntity> representedDefendants = List.of(respondedDefendant, awaitingDefendant);
        List<PartyEntity> awaitingDefendants = List.of(awaitingDefendant);
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        String organisationId = "org";

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, organisationId))
            .thenReturn(representedDefendants);
        when(legalRepPartySelectionService.filterDefendantsAwaitingResponse(CASE_REFERENCE, representedDefendants))
            .thenReturn(awaitingDefendants);
        when(legalRepPartySelectionService.hasSubmittedResponseForCurrentlySelectedParty(CASE_REFERENCE))
            .thenReturn(false);
        when(legalRepPartySelectionService.getDraftCaseData(CASE_REFERENCE, pcsCase, awaitingDefendant,
                                                            awaitingDefendants, organisationId))
            .thenReturn(pcsCase);

        // When
        underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // Then
        verify(legalRepPartySelectionService).getDraftCaseData(CASE_REFERENCE, pcsCase, awaitingDefendant,
                                                               awaitingDefendants, organisationId);
    }

    @Test
    void shouldAuthoriseRepresentedDefendantThatHasAlreadySubmittedAResponse() {
        // Given
        UUID defendantId = UUID.randomUUID();
        String organisationId = "ORG-123";

        PartyEntity defendant = PartyEntity.builder().id(defendantId).firstName("Sam").lastName("Defendant").build();
        defendant.setClaimPartyOrganisationList(List.of(
            ClaimPartyOrganisationEntity.builder()
                .party(defendant)
                .organisation(OrganisationEntity.builder().organisationId(organisationId).build())
                .active(YesOrNo.YES)
                .build()
        ));

        ClaimEntity claimEntity = ClaimEntity.builder().build();
        claimEntity.getClaimParties().add(ClaimPartyEntity.builder()
                                              .party(defendant)
                                              .role(PartyRole.DEFENDANT)
                                              .build());
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(claimEntity))
            .build();

        DefendantResponseRepository responseRepo = mock(DefendantResponseRepository.class);
        when(responseRepo.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, defendantId))
            .thenReturn(true);

        LegalRepForDefendantAccessValidator accessValidator =
            new LegalRepForDefendantAccessValidator(
                new DefendantPartyExtractor(claimPartyOrganisationRepository));

        LegalRepStartEventStrategy strategy = new LegalRepStartEventStrategy(
            pcsCaseService,
            accessValidator,
            new LegalRepPartySelectionService(
                mock(SelectedPartyRetriever.class),
                responseRepo,
                mock(DraftCaseDataService.class),
                mock(PossessionClaimResponseMapper.class),
                mock(PossessionClaimMerger.class),
                accessValidator,
                pcsCaseService),
            organisationService,
            tenancyLicenceView,
            noticeOfPossessionView,
            rentArrearsView);

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);

        // When
        PCSCase result = strategy.loadDraft(CASE_REFERENCE, PCSCase.builder().build());

        // Then
        assertThat(result.getAllLinkedDefendants()).hasSize(1);
        assertThat(result.getAllLinkedDefendants().getFirst().getValue().getLastName()).isEqualTo("Defendant");
        assertThat(result.getPossessionClaimResponse().getDefendantResponses().getStatus())
            .isEqualTo(DefendantResponseStatus.SUBMITTED);
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldExcludeRespondedDefendantsFromSelectionList() {
        // Given
        String organisationId = "ORG-123";
        UUID respondedId = UUID.randomUUID();
        UUID awaitingId1 = UUID.randomUUID();
        UUID awaitingId2 = UUID.randomUUID();

        PartyEntity responded = representedDefendant(respondedId, "Responded", organisationId);
        PartyEntity awaiting1 = representedDefendant(awaitingId1, "AwaitingOne", organisationId);
        PartyEntity awaiting2 = representedDefendant(awaitingId2, "AwaitingTwo", organisationId);

        ClaimEntity claimEntity = ClaimEntity.builder().build();
        Stream.of(responded, awaiting1, awaiting2).forEach(defendant ->
            claimEntity.getClaimParties().add(ClaimPartyEntity.builder()
                                                  .party(defendant)
                                                  .role(PartyRole.DEFENDANT)
                                                  .build()));
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(claimEntity))
            .build();

        DefendantResponseRepository responseRepo = mock(DefendantResponseRepository.class);
        when(responseRepo.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, respondedId)).thenReturn(true);
        when(responseRepo.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, awaitingId1)).thenReturn(false);
        when(responseRepo.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, awaitingId2)).thenReturn(false);

        LegalRepForDefendantAccessValidator accessValidator =
            new LegalRepForDefendantAccessValidator(
                new DefendantPartyExtractor(claimPartyOrganisationRepository));

        LegalRepStartEventStrategy strategy = new LegalRepStartEventStrategy(
            pcsCaseService,
            accessValidator,
            new LegalRepPartySelectionService(
                mock(SelectedPartyRetriever.class),
                responseRepo,
                mock(DraftCaseDataService.class),
                mock(PossessionClaimResponseMapper.class),
                mock(PossessionClaimMerger.class),
                accessValidator,
                pcsCaseService),
            organisationService,
            tenancyLicenceView,
            noticeOfPossessionView,
            rentArrearsView);

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);

        // When
        PCSCase result = strategy.loadDraft(CASE_REFERENCE, PCSCase.builder().build());

        // Then
        assertThat(result.getParties())
            .extracting(party -> party.getValue().getLastName())
            .containsExactlyInAnyOrder("AwaitingOne", "AwaitingTwo");
        assertThat(result.getAllLinkedDefendants())
            .extracting(party -> party.getValue().getLastName())
            .containsExactlyInAnyOrder("AwaitingOne", "AwaitingTwo");
    }

    @Test
    void shouldExposeAllRepresentedDefendantsWhenEveryRepresentedDefendantHasSubmitted() {
        // Given
        String organisationId = "ORG-123";
        UUID respondedId1 = UUID.randomUUID();
        UUID respondedId2 = UUID.randomUUID();

        PartyEntity responded1 = representedDefendant(respondedId1, "RespondedOne", organisationId);
        PartyEntity responded2 = representedDefendant(respondedId2, "RespondedTwo", organisationId);

        ClaimEntity claimEntity = ClaimEntity.builder().build();
        Stream.of(responded1, responded2).forEach(defendant ->
            claimEntity.getClaimParties().add(ClaimPartyEntity.builder()
                                                  .party(defendant)
                                                  .role(PartyRole.DEFENDANT)
                                                  .build()));
        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(claimEntity))
            .build();

        DefendantResponseRepository responseRepo = mock(DefendantResponseRepository.class);
        when(responseRepo.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, respondedId1)).thenReturn(true);
        when(responseRepo.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, respondedId2)).thenReturn(true);

        LegalRepForDefendantAccessValidator accessValidator =
            new LegalRepForDefendantAccessValidator(
                new DefendantPartyExtractor(claimPartyOrganisationRepository));

        LegalRepStartEventStrategy strategy = new LegalRepStartEventStrategy(
            pcsCaseService,
            accessValidator,
            new LegalRepPartySelectionService(
                mock(SelectedPartyRetriever.class),
                responseRepo,
                mock(DraftCaseDataService.class),
                mock(PossessionClaimResponseMapper.class),
                mock(PossessionClaimMerger.class),
                accessValidator,
                pcsCaseService),
            organisationService,
            tenancyLicenceView,
            noticeOfPossessionView,
            rentArrearsView);

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);

        // When
        PCSCase result = strategy.loadDraft(CASE_REFERENCE, PCSCase.builder().build());

        // Then
        assertThat(result.getAllLinkedDefendants())
            .extracting(party -> party.getValue().getLastName())
            .containsExactlyInAnyOrder("RespondedOne", "RespondedTwo");
        assertThat(result.getPossessionClaimResponse().getDefendantResponses().getStatus())
            .isEqualTo(DefendantResponseStatus.SUBMITTED);
    }

    private PartyEntity representedDefendant(UUID partyId, String lastName, String organisationId) {
        PartyEntity defendant = PartyEntity.builder().id(partyId).lastName(lastName).build();
        defendant.setClaimPartyOrganisationList(List.of(
            ClaimPartyOrganisationEntity.builder()
                .party(defendant)
                .organisation(OrganisationEntity.builder().organisationId(organisationId).build())
                .active(YesOrNo.YES)
                .build()
        ));
        return defendant;
    }

    @Test
    void shouldPropagateCaseAccessExceptionWhenOrganisationHasNoActiveRepresentation() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        String organisationId = "org";

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, organisationId))
            .thenThrow(new CaseAccessException("User is not linked as a defendant solicitor on this case"));

        // When / Then
        assertThatThrownBy(() -> underTest.loadDraft(CASE_REFERENCE, pcsCase))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant solicitor on this case");

        verify(legalRepPartySelectionService, never()).filterDefendantsAwaitingResponse(anyLong(), anyList());
        verify(legalRepPartySelectionService, never()).buildSubmittedResponseCase(any(), anyList());
    }

}
