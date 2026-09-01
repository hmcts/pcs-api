package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.LegalRepPartySelectionService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, organisationId, true))
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
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, organisationId, true))
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
    void shouldBuildSubmittedResponseWhenResponseAlreadySubmitted() {
        // Given
        UUID defendantId = UUID.randomUUID();
        UUID representativeId = UUID.randomUUID();
        PartyEntity defendantEntity = PartyEntity.builder().id(defendantId).build();
        List<PartyEntity> defendantParties = List.of(defendantEntity);
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        String organisationId = "org";

        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(organisationId);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, organisationId, false))
            .thenReturn(defendantParties);
        when(legalRepPartySelectionService.hasSubmittedResponseForCurrentlySelectedParty(CASE_REFERENCE))
            .thenReturn(true);

        PCSCase pcsCase = PCSCase.builder().build();
        // When
        underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // Then
        verify(legalRepPartySelectionService).buildSubmittedResponseCase(pcsCase, defendantParties);
    }

}
