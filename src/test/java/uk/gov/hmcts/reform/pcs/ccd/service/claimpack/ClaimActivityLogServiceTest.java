package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimActivityLogServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    @InjectMocks
    private ClaimActivityLogService claimActivityLogService;

    @Test
    void logsDocumentsCreatedSuccessAgainstTheClaimantParty() {
        PartyEntity claimant = mock(PartyEntity.class);
        stubCaseWithClaimant(claimant);

        claimActivityLogService.logGenerationSuccess(CASE_REFERENCE);

        ClaimActivityLogEntity saved = captureSaved();
        assertThat(saved.getActivityType()).isEqualTo(ClaimActivityType.DOCUMENTS_CREATED);
        assertThat(saved.getStatus()).isEqualTo(ClaimActivityStatus.SUCCESS);
        assertThat(saved.getParty()).isSameAs(claimant);
    }

    @Test
    void logsDocumentsCreatedFailure() {
        stubCaseWithClaimant(mock(PartyEntity.class));

        claimActivityLogService.logGenerationFailure(CASE_REFERENCE);

        assertThat(captureSaved().getStatus()).isEqualTo(ClaimActivityStatus.FAILURE);
    }

    @Test
    void logsWithNullPartyWhenNoClaimantOnTheCase() {
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        ClaimEntity claim = mock(ClaimEntity.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(pcsCase.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimParties()).thenReturn(List.of());

        claimActivityLogService.logGenerationSuccess(CASE_REFERENCE);

        assertThat(captureSaved().getParty()).isNull();
    }

    private void stubCaseWithClaimant(PartyEntity claimant) {
        PcsCaseEntity pcsCase = mock(PcsCaseEntity.class);
        ClaimEntity claim = mock(ClaimEntity.class);
        ClaimPartyEntity claimParty = mock(ClaimPartyEntity.class);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(pcsCase.getClaims()).thenReturn(List.of(claim));
        when(claim.getClaimParties()).thenReturn(List.of(claimParty));
        when(claimParty.getRole()).thenReturn(PartyRole.CLAIMANT);
        when(claimParty.getParty()).thenReturn(claimant);
    }

    private ClaimActivityLogEntity captureSaved() {
        ArgumentCaptor<ClaimActivityLogEntity> captor = ArgumentCaptor.forClass(ClaimActivityLogEntity.class);
        verify(claimActivityLogRepository).save(captor.capture());
        return captor.getValue();
    }
}
