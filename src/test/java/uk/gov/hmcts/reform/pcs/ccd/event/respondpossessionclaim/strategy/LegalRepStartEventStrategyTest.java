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
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.LegalRepresentativeRetriever;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private SecurityContextService securityContextService;

    @Mock
    private LegalRepPartySelectionService legalRepPartySelectionService;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @Mock
    private LegalRepresentativeRetriever legalRepresentativeRetriever;

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

        UUID userId = UUID.randomUUID();
        String organisationId = "org";
        UUID legalRepOrganisationId = UUID.randomUUID();
        List<PartyEntity> defendants = List.of(defendant);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(organisationId);
        when(legalRepresentativeRetriever.getLegalRepOrganisationIdForUser(CASE_REFERENCE, organisationId))
            .thenReturn(legalRepOrganisationId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, organisationId))
            .thenReturn(defendants);

        when(defendant.getId()).thenReturn(UUID.randomUUID());

        when(legalRepPartySelectionService.getDraftCaseData(CASE_REFERENCE, pcsCase,
                                                            defendant, defendants, legalRepOrganisationId))
            .thenReturn(pcsCase);

        // when
        PCSCase result = underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // then
        assertThat(result).isEqualTo(pcsCase);

        verify(legalRepPartySelectionService).validateResponseNotAlreadySubmitted(CASE_REFERENCE, defendant.getId());

        verify(legalRepPartySelectionService).getDraftCaseData(CASE_REFERENCE, pcsCase, defendant, defendants,
                                                               legalRepOrganisationId);
    }

    @Test
    void shouldLoadDraft_ForMultipleDefendants() {
        // given
        PCSCase pcsCase = mock(PCSCase.class);

        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        PartyEntity defendant1 = mock(PartyEntity.class);
        PartyEntity defendant2 = mock(PartyEntity.class);

        UUID userId = UUID.randomUUID();
        String organisationId = "org";
        UUID legalRepOrganisationId = UUID.randomUUID();
        List<PartyEntity> defendants = List.of(defendant1, defendant2);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(organisationId);
        when(legalRepresentativeRetriever.getLegalRepOrganisationIdForUser(CASE_REFERENCE, organisationId))
            .thenReturn(legalRepOrganisationId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, organisationId))
            .thenReturn(defendants);

        when(legalRepPartySelectionService.getDraft(pcsCase, defendants, CASE_REFERENCE, legalRepOrganisationId))
            .thenReturn(pcsCase);

        // when
        PCSCase result = underTest.loadDraft(CASE_REFERENCE, pcsCase);

        // then
        assertThat(result).isEqualTo(pcsCase);

        verify(legalRepPartySelectionService, never()).validateResponseNotAlreadySubmitted(anyLong(), any());

        verify(legalRepPartySelectionService).getDraft(pcsCase, defendants, CASE_REFERENCE, legalRepOrganisationId);
    }


}
