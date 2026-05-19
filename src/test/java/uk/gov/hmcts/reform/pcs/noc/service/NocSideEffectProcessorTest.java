package uk.gov.hmcts.reform.pcs.noc.service;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobEntity;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobStatus;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobType;
import uk.gov.hmcts.reform.pcs.noc.repository.NocSideEffectJobRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NocSideEffectProcessorTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private NocSideEffectJobRepository repository;
    @Mock
    private PcsCaseUserAccessService caseUserAccessService;

    private NocSideEffectProcessor underTest;

    @BeforeEach
    void setUp() {
        underTest = new NocSideEffectProcessor(repository, caseUserAccessService);
    }

    @Test
    void shouldCompleteGrantJob() {
        NocSideEffectJobEntity job = job(NocSideEffectJobType.GRANT_CASE_ROLE);
        when(repository.findById(JOB_ID)).thenReturn(Optional.of(job));

        boolean completed = underTest.process(JOB_ID);

        assertThat(completed).isTrue();
        assertThat(job.getStatus()).isEqualTo(NocSideEffectJobStatus.COMPLETED);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getCompletedAt()).isNotNull();
        verify(caseUserAccessService).grantCaseRole(job);
    }

    @Test
    void shouldMarkFailedJobForRetryWhenSideEffectFails() {
        NocSideEffectJobEntity job = job(NocSideEffectJobType.REVOKE_CASE_ROLE);
        when(repository.findById(JOB_ID)).thenReturn(Optional.of(job));
        doThrow(new IllegalStateException("ccd unavailable")).when(caseUserAccessService).revokeCaseRole(job);

        boolean completed = underTest.process(JOB_ID);

        assertThat(completed).isFalse();
        assertThat(job.getStatus()).isEqualTo(NocSideEffectJobStatus.FAILED);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getAvailableAt()).isNotNull();
        assertThat(job.getLastError()).isEqualTo("ccd unavailable");
    }

    private NocSideEffectJobEntity job(NocSideEffectJobType type) {
        return NocSideEffectJobEntity.builder()
            .id(JOB_ID)
            .type(type)
            .status(NocSideEffectJobStatus.PENDING)
            .build();
    }
}
