package uk.gov.hmcts.reform.pcs.enforcement.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AfterCommitTest {

    private final AtomicInteger runCount = new AtomicInteger();

    @AfterEach
    void clearSynchronisation() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRunImmediatelyWhenThereIsNoTransaction() {
        AfterCommit.run(runCount::incrementAndGet);

        assertThat(runCount).hasValue(1);
    }

    @Test
    void shouldWaitForTheCommitWhenThereIsATransaction() {
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.run(runCount::incrementAndGet);

        assertThat(runCount).hasValue(0);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(runCount).hasValue(1);
    }

    @Test
    void shouldNotRunWhenTheTransactionIsRolledBack() {
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.run(runCount::incrementAndGet);
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(runCount).hasValue(0);
    }
}
