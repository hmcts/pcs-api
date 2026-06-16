package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClaimActivityLogServiceTest {

    @Mock
    private ClaimActivityLogRepository claimActivityLogRepository;

    private ClaimActivityLogService underTest;

    @Captor
    private ArgumentCaptor<ClaimActivityLogEntity> captor;

    @BeforeEach
    void setUp() {
        underTest = new ClaimActivityLogService(claimActivityLogRepository);
    }

    @Test
    void shouldLogSuccessForParty() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();

        underTest.logSuccess(pcsCase, party, ClaimActivityType.DOCUMENTS_CREATED);

        verify(claimActivityLogRepository).save(captor.capture());
        ClaimActivityLogEntity saved = captor.getValue();
        assertThat(saved.getPcsCase()).isEqualTo(pcsCase);
        assertThat(saved.getParty()).isEqualTo(party);
        assertThat(saved.getActivityType()).isEqualTo(ClaimActivityType.DOCUMENTS_CREATED);
        assertThat(saved.getStatus()).isEqualTo(ClaimActivityStatus.SUCCESS);
    }

    @Test
    void shouldLogFailureForParty() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(UUID.randomUUID()).build();
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();

        underTest.logFailure(pcsCase, party, ClaimActivityType.DOCUMENTS_CREATED);

        verify(claimActivityLogRepository).save(captor.capture());
        ClaimActivityLogEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ClaimActivityStatus.FAILURE);
        assertThat(saved.getActivityType()).isEqualTo(ClaimActivityType.DOCUMENTS_CREATED);
    }
}
