package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeCaseDraftLoaderTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PossessionClaimResponseMapper responseMapper;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private DefendantResponseRepository defendantResponseRepository;
    @Mock
    private LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    @Mock
    private SecurityContextService securityContextService;

    private LegalRepresentativeCaseDraftLoader underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepresentativeCaseDraftLoader(
            pcsCaseService,
            responseMapper,
            draftCaseDataService,
            defendantResponseRepository,
            legalRepForDefendantAccessValidator,
            securityContextService
        );
    }

    @Test
    void shouldReturnRepresentedPartiesOnlyWhenNoPartyContextProvided() {
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();

        PCSCase caseData = PCSCase.builder().build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder()
            .id(representedPartyId)
            .firstName("Sam")
            .lastName("Defendant")
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty));

        PCSCase result = underTest.loadDraft(CASE_REFERENCE, caseData);

        assertThat(result.getPossessionClaimResponse()).isNull();
        assertThat(result.getParties()).hasSize(1);
        Party returnedParty = result.getParties().getFirst().getValue();
        assertThat(result.getParties().getFirst().getId()).isEqualTo(representedPartyId.toString());
        assertThat(returnedParty.getFirstName()).isEqualTo("Sam");
        assertThat(returnedParty.getLastName()).isEqualTo("Defendant");
        verify(draftCaseDataService, never()).hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId);
    }

    @Test
    void shouldInitializeDraftForSelectedRepresentedPartyWhenNoDraftExists() {
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();

        PCSCase caseData = PCSCase.builder().build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();
        PossessionClaimResponse response = PossessionClaimResponse.builder().build();
        caseData.setSelectedRespondingPartyId(representedPartyId.toString());

        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, representedPartyId))
            .thenReturn(false);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId))
            .thenReturn(false);
        when(responseMapper.mapFrom(caseData, representedParty)).thenReturn(response);

        PCSCase result = underTest.loadDraft(CASE_REFERENCE, caseData);

        assertThat(result.getPossessionClaimResponse()).isEqualTo(response);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim), eq(representedPartyId)
        );
    }

    @Test
    void shouldLoadDraftForSelectedRepresentedPartyWhenDraftExists() {
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();

        PCSCase caseData = PCSCase.builder().build();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();
        PossessionClaimResponse savedResponse = PossessionClaimResponse.builder().build();
        PCSCase savedDraft = PCSCase.builder()
            .possessionClaimResponse(savedResponse)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();
        caseData.setSelectedRespondingPartyId(representedPartyId.toString());

        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, representedPartyId))
            .thenReturn(false);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId))
            .thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim, representedPartyId))
            .thenReturn(Optional.of(savedDraft));
        when(responseMapper.buildPartyFromEntity(representedParty, caseData))
            .thenReturn(uk.gov.hmcts.reform.pcs.ccd.domain.Party.builder().build());

        PCSCase result = underTest.loadDraft(CASE_REFERENCE, caseData);

        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim), eq(representedPartyId)
        );
    }

    @Test
    void shouldRejectPartyOutsideRepresentedDefendants() {
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();
        UUID differentPartyId = UUID.randomUUID();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();

        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        PCSCase caseData = PCSCase.builder()
            .selectedRespondingPartyId(differentPartyId.toString())
            .build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty));

        assertThatThrownBy(() -> underTest.loadDraft(CASE_REFERENCE, caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldRejectSelectedPartyWhenResponseAlreadySubmitted() {
        UUID legalRepUserId = UUID.randomUUID();
        UUID representedPartyId = UUID.randomUUID();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().build();
        PartyEntity representedParty = PartyEntity.builder().id(representedPartyId).build();

        when(securityContextService.getCurrentUserId()).thenReturn(legalRepUserId);
        PCSCase caseData = PCSCase.builder()
            .selectedRespondingPartyId(representedPartyId.toString())
            .build();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, legalRepUserId))
            .thenReturn(List.of(representedParty));
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyId(CASE_REFERENCE, representedPartyId))
            .thenReturn(true);

        assertThatThrownBy(() -> underTest.loadDraft(CASE_REFERENCE, caseData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("A response has already been submitted for this case.");
    }
}
