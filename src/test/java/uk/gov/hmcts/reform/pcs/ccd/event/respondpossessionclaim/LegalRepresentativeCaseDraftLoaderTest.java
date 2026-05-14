package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeCaseDraftLoaderTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private LegalRepPartySelectionService legalRepPartySelectionService;

    private LegalRepresentativeCaseDraftLoader underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepresentativeCaseDraftLoader(pcsCaseService,
                                                           legalRepForDefendantAccessValidator,
                                                           securityContextService,
                                                           legalRepPartySelectionService);
    }

    @Test
    void shouldLoadDraftForSingleLinkedDefendant() {
        // given
        long caseReference = 12345L;
        UUID userId = UUID.randomUUID();

        PCSCase pcsCase = PCSCase.builder()
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .build();

        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .build();

        PCSCase expected = PCSCase.builder()
            .build();

        when(pcsCaseService.loadCase(caseReference)).thenReturn(caseEntity);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, userId)).thenReturn(List.of(
            defendant));
        when(legalRepPartySelectionService.getDraftCaseData(caseReference, pcsCase, defendant, true)).thenReturn(
            expected);

        // when
        PCSCase result = underTest.loadDraft(caseReference, pcsCase);

        // then
        assertThat(result).isEqualTo(expected);
        verify(legalRepPartySelectionService).getDraftCaseData(caseReference, pcsCase, defendant, true);
    }

    @Test
    void shouldLoadDraftForMultipleLinkedDefendants() {
        // given
        long caseReference = 12345L;
        UUID userId = UUID.randomUUID();

        PCSCase pcsCase = PCSCase.builder()
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .build();

        PartyEntity defendant1 = PartyEntity.builder()
            .id(UUID.randomUUID())
            .build();

        PartyEntity defendant2 = PartyEntity.builder()
            .id(UUID.randomUUID())
            .build();

        List<PartyEntity> defendants = List.of(defendant1, defendant2);

        PCSCase expected = PCSCase.builder()
            .build();

        when(pcsCaseService.loadCase(caseReference)).thenReturn(caseEntity);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, userId)).thenReturn(defendants);
        when(legalRepPartySelectionService.getDraft(pcsCase, defendants, caseReference)).thenReturn(expected);

        // when
        PCSCase result = underTest.loadDraft(caseReference, pcsCase);

        // then
        assertThat(result).isEqualTo(expected);
        verify(legalRepPartySelectionService).getDraft(pcsCase, defendants, caseReference);
    }
}
