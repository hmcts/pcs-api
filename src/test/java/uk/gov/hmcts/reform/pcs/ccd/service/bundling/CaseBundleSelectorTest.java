package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleExecutionContext;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobContext;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseBundleSelectorTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private CaseBundleRequestFactory requestFactory;
    @Mock
    private PlatformTransactionManager transactionManager;

    private CaseBundleSelector underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseBundleSelector(pcsCaseRepository, requestFactory, transactionManager);
    }

    @Test
    void shouldBuildTheRequestFromTheCaseAsItIsNowInAReadOnlyTransaction() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        BundleRequest request = mock(BundleRequest.class);
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(requestFactory.build(JOB_ID, pcsCase)).thenReturn(request);

        assertThat(underTest.select(jobContext())).isSameAs(request);

        ArgumentCaptor<TransactionDefinition> transaction = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(transaction.capture());
        assertThat(transaction.getValue().isReadOnly()).isTrue();
    }

    @Test
    void shouldFailWhenTheCaseNoLongerExists() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.empty());
        BundleJobContext context = jobContext();

        assertThatThrownBy(() -> underTest.select(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(CASE_ID.toString());
    }

    private static BundleJobContext jobContext() {
        return new BundleJobContext(JOB_ID, Optional.empty(), CaseBundleScope.selectorParameters(CASE_ID),
                                    BundleExecutionContext.builder().build());
    }
}
